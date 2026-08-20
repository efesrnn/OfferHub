package com.example.offerhub.screens.subscriber

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.R
import com.example.offerhub.components.CampaignCard
import com.example.offerhub.components.OfferHubTopBar
import com.example.offerhub.data.model.Campaign
import com.example.offerhub.navigation.Routes
import com.example.offerhub.navigation.AppNavigation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriberHomeScreen(
    firstName:String,
    campaigns:List<Campaign>,
    onCampaignClick:(String)->Unit,
    onAcceptedCampaignsClick:()-> Unit
) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(
        lazyListState = listState
    )
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(top=30.dp)
        ) {
            OfferHubTopBar(
                onMenuClick = {}
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Hello, $firstName",
                color=Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "Recommended for you",
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )
            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(campaigns) { campaign ->
                    CampaignCard(
                        campaign = campaign,
                        onClick = {
                            onCampaignClick(campaign.campaignId)
                        }
                    )
                }
            }
            Spacer(modifier=Modifier.height(28.dp))
            Text(
                text="Your Campaigns",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold ,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier=Modifier.height(18.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(120.dp)
                    .clickable {
                        onAcceptedCampaignsClick()
                    },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF4F6FF)
                )

            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "Accepted campaigns",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "View the offers you have accepted",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(34.dp))

            Text(
                text = "Recently rated",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "No rated campaigns yet",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

    @Preview
    @Composable
    fun SubscriberHomeScreenPreview()
    {
        /*SubscriberHomeScreen()*/
        val campaigns = listOf(
            Campaign(
                campaignId = "1",
                title = "20 GB Internet",
                description = "Personalized offer for you.",
                price = 249.90
            ),
            Campaign(
                campaignId = "2",
                title = "Social Media Plus",
                description = "More data for your favorite apps.",
                price = 199.90
            ),
            Campaign(
                campaignId = "3",
                title = "Weekend Package",
                description = "Extra internet for weekends.",
                price = 129.90
            )
        )

        SubscriberHomeScreen(
            firstName = "A",
            campaigns = campaigns,
            onCampaignClick = { _ -> },
            onAcceptedCampaignsClick = {})
    }

