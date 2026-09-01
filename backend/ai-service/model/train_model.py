"""
OfferHub AI Service — model eğitimi.

generate_data.py ile üretilen sentetik veriden iki model eğitir:
  1. Dönüşüm/öneri skoru modeli — Lojistik Regresyon (ikili sınıflandırma:
     converted 0/1), çıktısı sigmoid(w·x + b) = 0..1 arası "skor".
  2. Segment sınıflandırma modeli — Çok sınıflı Lojistik Regresyon
     (YUKSEK_DEGER / RISKLI_KAYIP / YENI_ABONE / PASIF).

Öğrenilen katsayılar (ağırlık + bias + standardizasyon için mean/std)
model_weights.json dosyasına yazılır. Java tarafı bu JSON'u okuyup aynı
standardizasyon + sigmoid/softmax işlemini runtime'da uygular — yani
gerçekten "öğrenilmiş" katsayılarla, girdiye göre değişen bir çıktı üretir.
"""
import json

import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.tree import DecisionTreeClassifier

TARIFFS = ["EKONOMIK", "STANDART", "PREMIUM"]
CAMPAIGN_TYPES = ["EK_PAKET", "TARIFE_YUKSELTME", "CIHAZ_FIRSATI", "SADAKAT"]
SEGMENTS = ["YUKSEK_DEGER", "RISKLI_KAYIP", "YENI_ABONE", "PASIF"]

NUMERIC_FEATURES = [
    "tenureMonths", "monthlyDataUsageGb", "monthlyVoiceMinutes",
    "monthlySpendTry", "pastAcceptedOffers", "pastDeclinedOffers",
    "complaintCount6m", "usageTrend",
]


def one_hot(df: pd.DataFrame, column: str, categories: list[str]) -> pd.DataFrame:
    for cat in categories:
        df[f"{column}_{cat}"] = (df[column] == cat).astype(int)
    return df


def build_score_model():
    df = pd.read_csv("offer_training_data.csv")
    df = one_hot(df, "currentTariff", TARIFFS)
    df = one_hot(df, "campaignType", CAMPAIGN_TYPES)

    feature_cols = (
        NUMERIC_FEATURES
        + [f"currentTariff_{t}" for t in TARIFFS]
        + [f"campaignType_{c}" for c in CAMPAIGN_TYPES]
    )
    X = df[feature_cols].to_numpy(dtype=float)
    y = df["converted"].to_numpy(dtype=int)

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    model = LogisticRegression(max_iter=2000, C=1.0)
    model.fit(X_train_scaled, y_train)

    train_acc = accuracy_score(y_train, model.predict(X_train_scaled))
    test_acc = accuracy_score(y_test, model.predict(X_test_scaled))
    print(f"[Skor modeli] train acc={train_acc:.3f}  test acc={test_acc:.3f}")

    return {
        "modelType": "logistic_regression",
        "featureOrder": feature_cols,
        "mean": scaler.mean_.tolist(),
        "std": scaler.scale_.tolist(),
        "weights": model.coef_[0].tolist(),
        "bias": float(model.intercept_[0]),
        "trainAccuracy": round(float(train_acc), 4),
        "testAccuracy": round(float(test_acc), 4),
    }


def build_segment_model():
    """Segment etiketleri eşik/koşul tabanlı kurallardan üretildiği için
    (bkz. generate_data.py:label_segment), bu problem doğası gereği
    DOĞRUSAL OLMAYAN bir sınıflandırma problemidir ("VE" koşulları, eşik
    değerleri). Lojistik regresyon (doğrusal) burada zayıf kalıyor; karar
    ağacı (Decision Tree) bu tür eşik tabanlı kuralları çok daha iyi
    yakalıyor. Yine sklearn ile GERÇEKTEN eğitiliyor, hardcoded değil."""
    df = pd.read_csv("subscriber_profiles.csv")
    df = one_hot(df, "currentTariff", TARIFFS)

    feature_cols = NUMERIC_FEATURES + [f"currentTariff_{t}" for t in TARIFFS]
    X = df[feature_cols].to_numpy(dtype=float)
    y = df["segment"].to_numpy()

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    model = DecisionTreeClassifier(max_depth=5, min_samples_leaf=6, random_state=42)
    model.fit(X_train, y_train)

    train_acc = accuracy_score(y_train, model.predict(X_train))
    test_acc = accuracy_score(y_test, model.predict(X_test))
    print(f"[Segment modeli - Karar Ağacı] train acc={train_acc:.3f}  test acc={test_acc:.3f}")

    tree = model.tree_
    classes = model.classes_.tolist()
    nodes = []
    for i in range(tree.node_count):
        is_leaf = tree.children_left[i] == tree.children_right[i]
        if is_leaf:
            class_counts = tree.value[i][0]
            predicted_idx = int(np.argmax(class_counts))
            nodes.append({
                "isLeaf": True,
                "predictedClass": classes[predicted_idx],
            })
        else:
            nodes.append({
                "isLeaf": False,
                "featureIndex": int(tree.feature[i]),
                "threshold": float(tree.threshold[i]),
                "left": int(tree.children_left[i]),
                "right": int(tree.children_right[i]),
            })

    return {
        "modelType": "decision_tree",
        "featureOrder": feature_cols,
        "nodes": nodes,
        "classes": classes,
        "trainAccuracy": round(float(train_acc), 4),
        "testAccuracy": round(float(test_acc), 4),
    }


def main():
    score_model = build_score_model()
    segment_model = build_segment_model()

    output = {
        "recommendationModel": score_model,
        "segmentModel": segment_model,
        "tariffs": TARIFFS,
        "campaignTypes": CAMPAIGN_TYPES,
        "segments": SEGMENTS,
    }

    with open("model_weights.json", "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)

    print("\nmodel_weights.json yazıldı.")


if __name__ == "__main__":
    main()
