package com.lura.moneymanager.daterangepicker

import android.content.Context
import android.text.format.DateFormat
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.*

open class TimeFormatter(val context: Context) {
  open fun printTime(dateTime: DateTime): String {
    val formatter = if (DateFormat.is24HourFormat(context))
      DateTimeFormat.forPattern("H:mm").withLocale(Locale.getDefault())
    else
      DateTimeFormat.forPattern("h:mm a").withLocale(Locale.getDefault())
    return formatter.print(dateTime)
  }
}
