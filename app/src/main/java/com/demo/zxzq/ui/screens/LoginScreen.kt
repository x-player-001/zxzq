package com.demo.zxzq.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.zxzq.ui.theme.ZxColors

/** 假登录页：随便输入点“登录”即进入已登录态。 */
@Composable
fun LoginScreen(onBack: () -> Unit = {}, onLoginSuccess: () -> Unit = {}) {
    var account by remember { mutableStateOf("88****6458") }
    var password by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(ZxColors.CardBg)
    ) {
        // 顶栏
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "返回",
                tint = ZxColors.TextPrimary,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(28.dp)
                    .clickable { onBack() }
            )
            Text(
                "登录交易", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = ZxColors.TextPrimary, modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(Modifier.height(32.dp))

        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(
                "交易登录", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                color = ZxColors.TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text("请输入交易账号与密码", fontSize = 14.sp, color = ZxColors.TextSecondary)

            Spacer(Modifier.height(32.dp))

            LoginField(
                value = account,
                onChange = { account = it },
                placeholder = "资金账号 / 手机号",
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(16.dp))
            LoginField(
                value = password,
                onChange = { password = it },
                placeholder = "交易密码",
                isPassword = true,
                keyboardType = KeyboardType.NumberPassword
            )

            Spacer(Modifier.height(32.dp))

            // 登录按钮：演示用固定密码 888888。
            val context = LocalContext.current
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(ZxColors.Brand, RoundedCornerShape(24.dp))
                    .clickable {
                        when {
                            account.isBlank() || password.isBlank() ->
                                Toast.makeText(context, "请输入交易账号与密码", Toast.LENGTH_SHORT).show()
                            password != "888888" ->
                                Toast.makeText(context, "交易密码错误", Toast.LENGTH_SHORT).show()
                            else -> onLoginSuccess()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("登 录", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("忘记密码", fontSize = 13.sp, color = ZxColors.TextSecondary)
                Text("遇到问题？", fontSize = 13.sp, color = ZxColors.LinkBlue)
            }

        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        TextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, color = ZxColors.TextTertiary, fontSize = 16.sp) },
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = ZxColors.Brand,
                unfocusedIndicatorColor = ZxColors.Divider,
                cursorColor = ZxColors.Brand,
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = ZxColors.TextPrimary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
