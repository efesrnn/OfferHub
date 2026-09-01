
import numpy as np
import pandas as pd

RNG_SEED = 42
N_SUBSCRIBERS = 220
CAMPAIGN_TYPES = ["EK_PAKET", "TARIFE_YUKSELTME", "CIHAZ_FIRSATI", "SADAKAT"]
TARIFFS = ["EKONOMIK", "STANDART", "PREMIUM"]

FIRST_NAMES = [
    "Ayşe", "Mehmet", "Fatma", "Ali", "Zeynep", "Mustafa", "Emine", "Hüseyin",
    "Hatice", "İbrahim", "Elif", "Ahmet", "Meryem", "Yusuf", "Özlem", "Murat",
    "Selin", "Emre", "Büşra", "Kerem", "Derya", "Burak", "Gül", "Onur",
]


def generate_subscribers(rng: np.random.Generator, n: int) -> pd.DataFrame:
    tenure_months = rng.integers(1, 85, size=n)
    monthly_data_usage_gb = np.clip(rng.gamma(shape=2.2, scale=6.0, size=n), 0.5, 80)
    monthly_voice_minutes = np.clip(rng.gamma(shape=2.0, scale=250, size=n), 0, 4000)
    tariff_idx = rng.choice(len(TARIFFS), size=n, p=[0.35, 0.45, 0.20])
    current_tariff = np.array(TARIFFS)[tariff_idx]

    # Harcama tarifeyle ve kullanımla ilişkili olsun (gerçekçilik için)
    tariff_base_spend = np.array([120, 260, 480])[tariff_idx]
    monthly_spend_try = np.clip(
        tariff_base_spend + monthly_data_usage_gb * 4.5 + rng.normal(0, 40, size=n),
        50, 1800,
    )

    # Geçmiş kabul/ret sayıları — daha uzun süredir abone olanlarda daha fazla geçmiş var
    exposure = np.clip(tenure_months / 6, 0, 14).astype(int)
    accept_rate_latent = rng.beta(2, 3, size=n)  # kişiye özgü gizli "kabul etme eğilimi"
    past_accepted_offers = rng.binomial(exposure, accept_rate_latent)
    past_declined_offers = exposure - past_accepted_offers

    complaint_count_6m = rng.poisson(lam=np.clip(1.2 - accept_rate_latent, 0.1, None), size=n)
    complaint_count_6m = np.clip(complaint_count_6m, 0, 8)

    # Kullanım trendi: -1 (hızla azalıyor) ... +1 (hızla artıyor)
    usage_trend = np.clip(rng.normal(0.05, 0.35, size=n), -1, 1)
    # Şikayeti çok olanların trendi genelde negatife çekilsin
    usage_trend -= complaint_count_6m * 0.08
    usage_trend = np.clip(usage_trend, -1, 1)

    subscriber_id = [f"SUB-{i+1:04d}" for i in range(n)]
    first_name = rng.choice(FIRST_NAMES, size=n)

    df = pd.DataFrame({
        "subscriberId": subscriber_id,
        "firstName": first_name,
        "tenureMonths": tenure_months,
        "monthlyDataUsageGb": monthly_data_usage_gb.round(2),
        "monthlyVoiceMinutes": monthly_voice_minutes.round(0).astype(int),
        "monthlySpendTry": monthly_spend_try.round(2),
        "currentTariff": current_tariff,
        "pastAcceptedOffers": past_accepted_offers,
        "pastDeclinedOffers": past_declined_offers,
        "complaintCount6m": complaint_count_6m,
        "usageTrend": usage_trend.round(3),
    })
    return df


def label_segments(subs: pd.DataFrame, rng: np.random.Generator) -> pd.Series:
    """Her abone için 4 segmente ait bir 'uygunluk skoru' hesaplanır; skorlar
    tüm popülasyona göre z-normalize edilir (aksi halde ölçek farkları bir
    segmenti sistematik olarak domine eder), sonra en yüksek skorlu segment
    seçilir (argmax). Böylece her satır gerçek, öğrenilebilir bir kurala göre
    etiketlenir. Sonda küçük bir gürültü payı eklenir (etiket kusursuzluğunu
    kırmak için)."""
    yeni_abone_raw = np.maximum(0.0, 6 - subs["tenureMonths"])
    risk_raw = (
        subs["complaintCount6m"] * 0.7
        + np.maximum(0.0, -subs["usageTrend"]) * 3.0
        + np.where(subs["pastDeclinedOffers"] > subs["pastAcceptedOffers"], 0.5, 0.0)
    )
    deger_raw = (
        subs["monthlySpendTry"] / 300.0
        + subs["monthlyDataUsageGb"] / 12.0
        + (subs["pastAcceptedOffers"] - subs["pastDeclinedOffers"]) * 0.25
        + subs["tenureMonths"] / 60.0
    )
    pasif_raw = (
        np.maximum(0.0, 8 - subs["monthlyDataUsageGb"]) / 8.0
        + np.maximum(0.0, 250 - subs["monthlySpendTry"]) / 250.0
        + np.where(
            (subs["pastAcceptedOffers"] == 0) & (subs["pastDeclinedOffers"] == 0), 0.8, 0.0
        )
    )

    def zscore(arr: pd.Series) -> np.ndarray:
        arr = arr.to_numpy(dtype=float)
        return (arr - arr.mean()) / (arr.std() + 1e-9)

    scores = np.column_stack([
        zscore(deger_raw),   # YUKSEK_DEGER
        zscore(risk_raw),    # RISKLI_KAYIP
        zscore(yeni_abone_raw),  # YENI_ABONE
        zscore(pasif_raw),   # PASIF
    ])
    labels = np.array(["YUKSEK_DEGER", "RISKLI_KAYIP", "YENI_ABONE", "PASIF"])
    base = labels[np.argmax(scores, axis=1)]

    # Etiket gürültüsü: %7 ihtimalle rastgele bir komşu segmente kaydır.
    noise_mask = rng.random(len(base)) < 0.07
    for idx in np.where(noise_mask)[0]:
        base[idx] = rng.choice([s for s in labels if s != base[idx]])

    return pd.Series(base, index=subs.index)


def campaign_match_bonus(row: pd.Series, campaign_type: str) -> float:
    if campaign_type == "EK_PAKET":
        return 0.35 * np.clip((row["monthlyDataUsageGb"] - 12) / 20, -1, 1)
    if campaign_type == "TARIFE_YUKSELTME":
        spend_pressure = 0.35 * np.clip((row["monthlyDataUsageGb"] - 18) / 25, -1, 1)
        tariff_room = 0.15 if row["currentTariff"] != "PREMIUM" else -0.10
        return spend_pressure + tariff_room
    if campaign_type == "CIHAZ_FIRSATI":
        loyalty = 0.30 * np.clip((row["tenureMonths"] - 12) / 40, -1, 1)
        spend_ok = 0.10 * np.clip((row["monthlySpendTry"] - 250) / 400, -1, 1)
        return loyalty + spend_ok
    if campaign_type == "SADAKAT":
        return 0.30 * np.clip((row["tenureMonths"] - 6) / 50, -1, 1)
    return 0.0


def generate_offer_training_data(subs: pd.DataFrame, rng: np.random.Generator) -> pd.DataFrame:
    rows = []
    for _, row in subs.iterrows():
        history_total = row["pastAcceptedOffers"] + row["pastDeclinedOffers"]
        accept_rate = (row["pastAcceptedOffers"] + 1) / (history_total + 2)  # Laplace düzeltmesi

        for campaign_type in CAMPAIGN_TYPES:
            base_prob = 0.30 + 0.45 * accept_rate
            base_prob += campaign_match_bonus(row, campaign_type)
            base_prob -= 0.06 * row["complaintCount6m"]
            base_prob += 0.15 * row["usageTrend"]
            base_prob = float(np.clip(base_prob, 0.03, 0.97))

            # Gürültülü gerçek olasılık + o olasılıktan örneklenmiş ikili sonuç
            noisy_prob = float(np.clip(base_prob + rng.normal(0, 0.08), 0.01, 0.99))
            converted = int(rng.random() < noisy_prob)

            rows.append({
                "subscriberId": row["subscriberId"],
                "campaignType": campaign_type,
                "tenureMonths": row["tenureMonths"],
                "monthlyDataUsageGb": row["monthlyDataUsageGb"],
                "monthlyVoiceMinutes": row["monthlyVoiceMinutes"],
                "monthlySpendTry": row["monthlySpendTry"],
                "currentTariff": row["currentTariff"],
                "pastAcceptedOffers": row["pastAcceptedOffers"],
                "pastDeclinedOffers": row["pastDeclinedOffers"],
                "complaintCount6m": row["complaintCount6m"],
                "usageTrend": row["usageTrend"],
                "trueConversionProbability": round(noisy_prob, 4),
                "converted": converted,
            })
    return pd.DataFrame(rows)


def main():
    rng = np.random.default_rng(RNG_SEED)
    subs = generate_subscribers(rng, N_SUBSCRIBERS)
    subs["segment"] = label_segments(subs, rng)

    offers = generate_offer_training_data(subs, rng)

    subs.to_csv("subscriber_profiles.csv", index=False, encoding="utf-8-sig")
    offers.to_csv("offer_training_data.csv", index=False, encoding="utf-8-sig")

    print(f"subscriber_profiles.csv: {len(subs)} satır")
    print(subs["segment"].value_counts())
    print()
    print(f"offer_training_data.csv: {len(offers)} satır")
    print(offers["converted"].value_counts(normalize=True))


if __name__ == "__main__":
    main()
