package com.example.offerhub.components

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
    keyboardType: KeyboardType= KeyboardType.Text,
    visualTransformation: VisualTransformation= VisualTransformation.None,
    isError: Boolean=false,
    errorMessage:String?=null,
    onFocusChanged: (Boolean) -> Unit = {}
)
{
 OutlinedTextField(
     value=value,
     onValueChange=onValueChange,  //?
     label={
         Text(text=label)
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
fun gsmTextFieldComponent(
    value: String,
    onValueChange:(String)->Unit,
    isError: Boolean=false,
    errorMessage: String?=null
) {
    OutlinedTextField(
        value=value,
        onValueChange={ newValue->

            val digitsOnly=newValue.filter{it.isDigit()}
            if(digitsOnly.length<=10)
            {
                onValueChange(digitsOnly)
            }
        },
        label={ Text("GSM")},
        prefix={Text("+90 ")},
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone
        ),
        isError=isError,
        supportingText = {
            if(isError&&errorMessage !=null){
                Text(errorMessage)
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

}
@Composable
fun EmailTextFieldComponent(
    value: String,
    onValueChange: (String) -> Unit,
    emailTouched: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    required:Boolean=true
) {
    val emailIsInvalid =
        value.isBlank() ||
                !Patterns.EMAIL_ADDRESS.matcher(value).matches()

    TextFieldComponent(
        value = value,
        onValueChange = onValueChange,
        label = "Email",
        keyboardType = KeyboardType.Email,
        onFocusChanged = onFocusChanged,
        isError = emailTouched && emailIsInvalid,
        errorMessage = when {
            value.isBlank() -> "Email cannot be empty"
            else -> "Please enter a valid email address"
        }
    )
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


