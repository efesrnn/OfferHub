package com.example.offerhub.components

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.ui.theme.Primary
import com.example.offerhub.ui.theme.Secondary

@Composable
fun TextFieldComponent(
    value:String,
    onValueChange:(String)->Unit,
    label:String,
    prefix:String,
    keyboardType: KeyboardType= KeyboardType.Text,
    visualTransformation: VisualTransformation= VisualTransformation.None,
    isError: Boolean=false,
    errorMessage:String?=null,
    onFocusChanged: (Boolean) -> Unit = {}
)
{
 OutlinedTextField(
     value=value,
     onValueChange = onValueChange,
     label={
         Text(text=label)
     },
     prefix = {
         if (prefix.isNotEmpty()) {
             Text(text = prefix)
         }
     },
     keyboardOptions = KeyboardOptions(
         keyboardType=keyboardType
     ),
     visualTransformation=visualTransformation,
     isError=isError,
     supportingText = {
         if(isError && errorMessage!=null)
         {
             Text(text=errorMessage)
         }
     },
     singleLine=true,  //?
     modifier=Modifier.fillMaxWidth()
 )
}

@Composable
fun AuthButton(   text:String,
                  onClick:()->Unit)
{
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        colors= ButtonDefaults.buttonColors(Color.Transparent)
    ){
        Box(modifier=Modifier
            .fillMaxWidth()
            .heightIn(70.dp)
            .background(
                brush=Brush.horizontalGradient(listOf(Secondary, Primary)),
                shape= RoundedCornerShape(50.dp)
            ),
            contentAlignment= Alignment.Center){
            Text(text=text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ClickableText(text:String,
    onClick: () -> Unit
) {
    var clicked by remember {
        mutableStateOf(false)
    }

    Text(
        text = text,
        fontSize = 14.sp,
        textDecoration = TextDecoration.Underline,
        color = if (clicked) {
            Color.DarkGray
        } else {
            Color.Gray
        },
        modifier = Modifier.clickable {
            clicked = true
            onClick()
        }
    )
}

@Composable
fun PasswordRequirements(
    password: String
) {
    val isValid =
        password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isDigit() } &&
                password.any { !it.isLetterOrDigit() }

    Text(
        text = if (isValid) {
            "✓ Your password meets the requirements"
        } else {
            "ⓘ Your password must be at least 8 characters and include an uppercase letter, a number, and a special character."
        },
        fontSize = 12.sp,
        color = if (isValid) {
            Color(0xFF2E7D32)
        } else {
            Color.Gray
        }
    )
}





