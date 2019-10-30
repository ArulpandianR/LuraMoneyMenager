package com.lura.moneymanager.coroutines

import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import com.lura.moneymanager.HomeActivity
import com.lura.moneymanager.model.MessageData
import com.lura.moneymanager.model.MessageDataResponse
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class MessageService {

    var smsFinal = ArrayList<MessageData>()

    private val amountPattern =
        "(?i)(?:(?:RS|INR|MRP)\\.?\\s?)(\\d+(:?\\,\\d+)?(\\,\\d+)?(\\.\\d{1,2})?)"
    private val bankNamePattern =
        "(?i)(?:\\sat\\s|on\\s|in\\*)([A-Za-z0-9]*\\s?-?\\s?[A-Za-z0-9]*\\s?-?\\.?)"
    private val cardDetailPattern =
        "(?i)(?:\\smade on|ur|made a\\s|in\\*)([A-Za-z]*\\s?-?\\s[A-Za-z]*\\s?-?\\s[A-Za-z]*\\s?-?)"
    var debited = 0.0
    var credited = 0.0

    @WorkerThread
    fun readAllMessage(
        activity: HomeActivity, startDate: String,
        endDate: String,
        bankSeletedItem: String,
        statusSelcetedItem: String
    ): MessageDataResponse {
        val uriSMSURI = Uri.parse("content://sms/")
        val cur = activity.contentResolver.query(uriSMSURI, null, null, null, null)!!
        val format1 = SimpleDateFormat("dd/MM/yyyy")
        smsFinal.clear()
        debited = 0.0
        credited = 0.0
        while (cur.moveToNext()) {
            //String address = cur.getString(cur.getColumnIndex("address"));
            val body = cur.getString(cur.getColumnIndexOrThrow("body"))
            val address = cur.getString(cur.getColumnIndexOrThrow("address"))
            val millis = cur.getString(cur.getColumnIndexOrThrow("date"))
            var name: String = ""//getContactName(address, this)

            val timestamp = java.lang.Long.parseLong(millis)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            Log.d("date", calendar.toString())

            var amount = ""
            var bankName = ""
            var cardName = ""
            var tranactionText = ""

            var amountPattern: Pattern = Pattern.compile(amountPattern)
            val amountMatcher = amountPattern.matcher(body)
            if (amountMatcher.find()) {
                amount = body.substring(amountMatcher.start(), amountMatcher.end())
            }

            var bankPattern: Pattern = Pattern.compile(bankNamePattern)
            val bankMatcher = bankPattern.matcher(body)
            if (bankMatcher.find()) {
                bankName = body.substring(bankMatcher.start(), bankMatcher.end())
            }

            var cardPattern: Pattern = Pattern.compile(cardDetailPattern)
            val cardMatcher = cardPattern.matcher(body)
            if (cardMatcher.find()) {
                cardName = body.substring(cardMatcher.start(), cardMatcher.end())
            }

            if (!amount.isNullOrEmpty() && !bankName.isNullOrEmpty() && address.contains(
                    bankSeletedItem, ignoreCase = true
                ) && isDateValid(calendar.time, startDate, endDate) && isStatusValid(
                    body,
                    statusSelcetedItem
                )
            ) {

                val re = Regex("[^0-9.]")
                var amountDouble = re.replace(amount.replace("Rs.", ""), "")
                if (body.contains("debited")) {
                    debited += amountDouble.toDouble()
                    tranactionText = "Debited"
                }
                if (body.contains("credited") || body.contains("deposited")) {
                    credited += amountDouble.toDouble()
                    tranactionText = "credited"
                }
                /*traction.text = "Credited Amount " + String.format(
                   "%.0f",
                   credited
               ) + "\n Debited Amount " + String.format("%.0f", debited)
               var bodyText =
                    "Bank Name  $bankName\n Card Details $cardName \n Status $tranactionText" +
                            "\n  Amount $amount \n Date " + format1.format(calendar.time)*/
                smsFinal.add(
                    MessageData(
                        address,
                        tranactionText,
                        amount,
                        format1.format(calendar.time)
                    )
                )
            }
        }

        return MessageDataResponse(smsFinal, credited,debited)
    }


    private fun isDateValid(giveDate: Date, startDate: String, endDate: String): Boolean {
        var start = SimpleDateFormat("dd/MM/yyyy").parse(startDate)
        var end = SimpleDateFormat("dd/MM/yyyy").parse(endDate)
        return giveDate.after(start) && giveDate.before(end)
    }

    private fun isStatusValid(body: String, statusSelcetedItem: String): Boolean {
        return if (statusSelcetedItem.isNullOrEmpty() || statusSelcetedItem.contains(
                "All",
                ignoreCase = true
            )
        ) {
            (body.contains("deposited", ignoreCase = true) || body.contains(
                "credited",
                ignoreCase = true
            ) || body.contains("debited", ignoreCase = true))
        } else {
            if (statusSelcetedItem.contains("debited", ignoreCase = true)) {
                body.contains("debited", ignoreCase = true)
            } else {
                (body.contains("deposited", ignoreCase = true) || body.contains(
                    "credited",
                    ignoreCase = true
                ))
            }
        }
    }
}