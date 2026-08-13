package com.og

import android.app.Application
import com.og.data.OgDatabase

class OgApp : Application() {
    val db by lazy { OgDatabase.get(this) }
}
