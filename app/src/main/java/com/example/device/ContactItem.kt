package com.example.device

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val type: String = "Mobile"
)
