package com.jnano.jngcsmsapp.utils

data class ResponseObject<T>(val error: Boolean, val message: String, val data: T)
