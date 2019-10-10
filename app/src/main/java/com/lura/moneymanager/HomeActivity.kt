package com.lura.moneymanager

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lura.moneymanager.adapter.MessageAdapter
import com.lura.moneymanager.daterangepicker.DateTimeRangePickerActivityRedesign
import com.lura.moneymanager.daterangepicker.DateTimeRangePickerViewModel
import com.lura.moneymanager.model.MessageData
import net.danlew.android.joda.JodaTimeAndroid
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern


class HomeActivity : AppCompatActivity() {
    lateinit var recyclerView: RecyclerView
    lateinit var traction: TextView
    lateinit var date: TextView
    private lateinit var bankSpinner: AppCompatSpinner
    private lateinit var statusSpinner: AppCompatSpinner

    var smsFinal = ArrayList<MessageData>()
    var RQC_PICK_DATE_TIME_RANGE = 101
    private val amountPattern =
        "(?i)(?:(?:RS|INR|MRP)\\.?\\s?)(\\d+(:?\\,\\d+)?(\\,\\d+)?(\\.\\d{1,2})?)"
    private val bankNamePattern =
        "(?i)(?:\\sat\\s|on\\s|in\\*)([A-Za-z0-9]*\\s?-?\\s?[A-Za-z0-9]*\\s?-?\\.?)"
    private val cardDetailPattern =
        "(?i)(?:\\smade on|ur|made a\\s|in\\*)([A-Za-z]*\\s?-?\\s[A-Za-z]*\\s?-?\\s[A-Za-z]*\\s?-?)"
    var debited = 0.0
    var credited = 0.0
    var tranactionText = ""
    var startDate = "1/9/2019"
    var endDate = "24/9/2019"
    var bankArrayItem = ""
    var statusSelcetedItem = ""

    var statusArray = arrayOf(
        "All",
        "credited",
        "debited"
    )

    var bankArray = arrayOf(
        "HDFC",
        "Axis",
        "ICICI",
        "CITY",
        "KOTAK",
        "IDBI",
        "INDUSIND",
        "KARUR",
        "CANARA",
        "SBI",
        "INDIAN"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        JodaTimeAndroid.init(this)
        recyclerView = findViewById<View>(R.id.recyclerView) as RecyclerView
        traction = findViewById<View>(R.id.traction) as TextView
        date = findViewById<View>(R.id.date) as TextView
        bankSpinner = findViewById<View>(R.id.bankSpinner) as AppCompatSpinner
        statusSpinner = findViewById<View>(R.id.statusSpinner) as AppCompatSpinner


        startDate = SimpleDateFormat("dd/MM/yyyy").format(DateTime.now().minusMonths(1).millis)
        endDate = SimpleDateFormat("dd/MM/yyyy").format(DateTime.now().millis)

        date.text = "Start date: $startDate \n End Date  :$endDate"

        bankSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_checked, bankArray)
        statusSpinner.adapter= ArrayAdapter(this, android.R.layout.simple_list_item_checked, statusArray)

        recyclerView.layoutManager = LinearLayoutManager(this@HomeActivity)
        if (!checkPermission()) {
            requestPermission(PERMISSION_REQUEST_CODE)
        }
        bankArrayItem = bankArray[0]
        statusSelcetedItem = statusArray[0]

        bankSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                bankArrayItem = bankArray[position]
                getMoneyDetails()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }
        }

        statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                statusSelcetedItem = statusArray[position]
                getMoneyDetails()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }
        }

    }

    private fun callDatePicker() {

        val intent = DateTimeRangePickerActivityRedesign.newIntent(
            this,
            TimeZone.getDefault(),
            DateTime.now().minusMonths(1).millis,
            DateTime.now().millis
        )
        startActivityForResult(intent, RQC_PICK_DATE_TIME_RANGE)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = getMenuInflater()
        menuInflater.inflate(R.menu.date_home_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> this.onBackPressed()
            R.id.date_menu -> callDatePicker()
        }
        return true
    }

    fun readAllMessage(): ArrayList<MessageData> {
        val uriSMSURI = Uri.parse("content://sms/")
        val cur = contentResolver.query(uriSMSURI, null, null, null, null)!!
        val format1 = SimpleDateFormat("dd/MM/yyyy")
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
                    bankArrayItem, ignoreCase = true) && isDateValid(calendar.time) &&isStatusValid(body)) {

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
                traction.text = "Credited Amount " + String.format(
                    "%.0f",
                    credited
                ) + "\n Debited Amount " + String.format("%.0f", debited)
                /* var bodyText =
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
            /*val timestamp = java.lang.Long.parseLong(millis)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            val finaldate = calendar.time
            Log.d("date", finaldate.toString())


            val rightNow = Calendar.getInstance()
            val rightnowdate: Date = rightNow.time
            Log.d("date", rightnowdate.toString())


            val hours: Long = Diff(rightnowdate, finaldate)
            Log.d("diff", hours.toString())

            if (abs(hours) < 1) {
                if (name == "" || TextUtils.isEmpty(name)) {
                    name = address
                }
                smsZero.add(MessageData(body, name, hours))
            }
            if (abs(hours) < 2 && abs(hours) >= 1) {
                if (name == "" || TextUtils.isEmpty(name)) {
                    name = address
                }
                smsOne.add(MessageData(body, name, hours))
            }
            if (abs(hours) < 3 && abs(hours) >= 2) {
                if (name == "" || TextUtils.isEmpty(name)) {
                    name = address
                }
                smsTwo.add(MessageData(body, name, hours))
            }
            if (abs(hours) < 6 && abs(hours) >= 3) {
                if (name == "" || TextUtils.isEmpty(name)) {
                    name = address
                }
                smsThree.add(MessageData(body, name, hours))
            }
            if (abs(hours) < 12 && abs(hours) >= 6) {
                if (name == "" || TextUtils.isEmpty(name)) {
                    name = address
                }
                smsSix.add(MessageData(body, name, hours))
            }
            if (abs(hours) < 24 && abs(hours) >= 12) {
                if (name == "" || TextUtils.isEmpty(name)) {
                    name = address
                }
                smsTwelve.add(MessageData(body, name, hours))
            }
            if (abs(hours) >= 24 && abs(hours) < 36) {
                if (name == "" || TextUtils.isEmpty(name)) {
                    name = address
                }
                smsDay.add(MessageData(body, name, hours))
            }*/
        }
        /*  smsFinal.add(MessageData("", "0 hours ago", 0))
          smsFinal.addAll(smsZero)
          smsFinal.add(MessageData("", "1 hours ago", 1))
          smsFinal.addAll(smsOne)
          smsFinal.add(MessageData("", "2 hours ago", 2))
          smsFinal.addAll(smsTwo)
          smsFinal.add(MessageData("", "3 hours ago", 3))
          smsFinal.addAll(smsThree)
          smsFinal.add(MessageData("", "6 hours ago", 6))
          smsFinal.addAll(smsSix)
          smsFinal.add(MessageData("", "12 hours ago", 12))
          smsFinal.addAll(smsTwelve)
          smsFinal.add(MessageData("", "1 Day ago", 24))
          smsFinal.addAll(smsDay)*/
        return smsFinal
    }

    fun Diff(date1: Date, date2: Date): Long {
        val mill_to_hour: Int = 1000 * 60 * 60
        return (date1.time - date2.time) / mill_to_hour
    }

    fun checkPermission(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_SMS)
        val result2 =
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECEIVE_SMS)
        val result3 =
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CONTACTS)

        return result == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED && result3 == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(int: Int) {
        ActivityCompat.requestPermissions(
            this,
            arrayOf<String>(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_CONTACTS
            ),
            int
        )

    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> if (grantResults.size > 0) {

                val readSms = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val recieveSms = grantResults[1] == PackageManager.PERMISSION_GRANTED
                val readcontact = grantResults[2] == PackageManager.PERMISSION_GRANTED


                if (readSms && readcontact && recieveSms) {

                    Log.d("perm grant", "Permission Granted")
                } else {

                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS) && shouldShowRequestPermissionRationale(
                                Manifest.permission.RECEIVE_SMS
                            ) && shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)
                        ) {
                            showMessageOKCancel("Please allow access to both the permissions",
                                object : DialogInterface.OnClickListener {
                                    override fun onClick(dialog: DialogInterface, which: Int) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermissions(
                                                arrayOf<String>(
                                                    Manifest.permission.READ_SMS,
                                                    Manifest.permission.RECEIVE_SMS,
                                                    Manifest.permission.READ_CONTACTS
                                                ),
                                                PERMISSION_REQUEST_CODE
                                            )
                                        }
                                    }
                                })
                            return
                        }
                    }

                }
            }
        }
    }

    fun getContactName(phoneNumber: String, context: Context): String {
        if (phoneNumber.isNullOrEmpty()) {
            return ""
        }
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        var contactName = ""
        val cursor = context.getContentResolver().query(uri, projection, null, null, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                contactName = cursor.getString(0)
            }
            cursor.close()
        }

        return contactName
    }

    override fun onResume() {
        super.onResume()
        getMoneyDetails()
    }


    private fun getMoneyDetails() {

        if (!checkPermission()) {
            requestPermission(PERMISSION_REQUEST_CODE)
        } else {
//            registerReceiver(broadCastReceiver,  IntentFilter("call_method"));
            //  progress.visibility = View.VISIBLE
            //recyclerView.visibility = View.GONE
            smsFinal.clear()
            debited = 0.0
            credited = 0.0
            val messageList: List<MessageData>

            messageList = readAllMessage()
            if (!messageList.isNullOrEmpty()) {
                val messagecsv = ArrayList<MessageData>()

                for (i in messageList.indices) {
                    messagecsv.add(messageList[i])
                }
                Log.d("list", messagecsv.toString())

                val smsAdapter = MessageAdapter(messagecsv, this)
                //recyclerView.adapter = null
                recyclerView.adapter = smsAdapter
                recyclerView.adapter?.notifyDataSetChanged()
            } else {
                debited = 0.0
                credited = 0.0
                recyclerView.adapter = null
            }
            // progress.visibility = View.GONE
            // recyclerView.visibility = View.VISIBLE
        }
    }

    fun showMessageOKCancel(message: String, okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(applicationContext)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    companion object {

        private val PERMISSION_REQUEST_CODE = 123
    }

    private fun isDateValid(giveDate: Date): Boolean {
        var start = SimpleDateFormat("dd/MM/yyyy").parse(startDate)
        var end = SimpleDateFormat("dd/MM/yyyy").parse(endDate)
        return giveDate.after(start) && giveDate.before(end)
    }

    private fun isStatusValid(body: String): Boolean {
        return if (statusSelcetedItem.isNullOrEmpty() || statusSelcetedItem.contains("All")) {
            (body.contains("deposited") || body.contains("credited") || body.contains("debited"))
        } else {
            if (statusSelcetedItem.contains("debited")) {
                body.contains("debited")
            } else {
                (body.contains("deposited") || body.contains("credited"))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == RQC_PICK_DATE_TIME_RANGE) {

            val startTime =
                data?.getLongExtra(DateTimeRangePickerViewModel.KEY_START_TIME_IN_MILLIS, 0L)
            val endTime =
                data?.getLongExtra(DateTimeRangePickerViewModel.KEY_END_TIME_IN_MILLIS, 0L)
            val timeZone = data?.getStringExtra(DateTimeRangePickerViewModel.KEY_TIME_ZONE)

            startDate = DateTimeFormat.forPattern("dd/MM/yyyy").withLocale(Locale.getDefault())
                .print(startTime!!)
            endDate = DateTimeFormat.forPattern("dd/MM/yyyy").withLocale(Locale.getDefault())
                .print(endTime!!)
            date.text = "Start date: $startDate \n End Date  :$endDate"

            getMoneyDetails()
        }

    }

}

