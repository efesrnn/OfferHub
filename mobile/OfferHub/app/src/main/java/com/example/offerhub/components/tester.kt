package com.example.offerhub.components

import android.R.attr.text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.ui.theme.Primary
import com.example.offerhub.ui.theme.Secondary

@Composable
fun RoleButton(   text:String,
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