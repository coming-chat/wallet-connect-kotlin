package com.imtianx.walletconnect.exceptions

import com.google.gson.JsonParseException

class RequiredFieldException(val field: String = "") : JsonParseException("'$field' is required")