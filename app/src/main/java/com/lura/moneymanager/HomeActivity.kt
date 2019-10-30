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
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lura.moneymanager.adapter.MessageAdapter
import com.lura.moneymanager.coroutines.MessageService
import com.lura.moneymanager.daterangepicker.DateTimeRangePickerActivityRedesign
import com.lura.moneymanager.daterangepicker.DateTimeRangePickerViewModel
import com.lura.moneymanager.model.MessageData
import com.lura.moneymanager.model.MessageDataResponse
import kotlinx.coroutines.*
import net.danlew.android.joda.JodaTimeAndroid
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext


class HomeActivity : AppCompatActivity(), CoroutineScope {
    lateinit var recyclerView: RecyclerView
    lateinit var progressBar: ProgressBar
    lateinit var traction: TextView
    lateinit var date: TextView
    private lateinit var bankSpinner: AppCompatSpinner
    private lateinit var statusSpinner: AppCompatSpinner
    var RQC_PICK_DATE_TIME_RANGE = 101

    var startDate = "1/9/2019"
    var endDate = "24/9/2019"
    var bankSeletedItem = ""
    var statusSelcetedItem = ""
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var job: Job

    private val messageService = MessageService()

    var statusArray = arrayOf(
        "All",
        "Credited",
        "Debited"
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
        progressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        job = Job()

        startDate = SimpleDateFormat("dd/MM/yyyy").format(DateTime.now().minusMonths(1).millis)
        endDate = SimpleDateFormat("dd/MM/yyyy").format(DateTime.now().millis)

        date.text = "Start date: $startDate \n End Date  :$endDate"

        bankSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_checked, bankArray)
        statusSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_checked, statusArray)

        recyclerView.layoutManager = LinearLayoutManager(this@HomeActivity)
        if (!checkPermission()) {
            requestPermission(PERMISSION_REQUEST_CODE)
        }
        bankSeletedItem = bankArray[0]
        statusSelcetedItem = statusArray[0]


        bankSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                bankSeletedItem = bankArray[position]
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

            launch {
                progressBar.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                val messageList: MutableList<MessageData>

                var messageDataResponse: MessageDataResponse = withContext(Dispatchers.IO) {
                    messageService.readAllMessage(
                        this@HomeActivity,
                        startDate, endDate, bankSeletedItem, statusSelcetedItem
                    )
                }
                recyclerView.adapter = null
                messageList = messageDataResponse.messageList

                if (!messageList.isNullOrEmpty()) {
                    val messagecsv = ArrayList<MessageData>()

                    for (i in messageList.indices) {
                        messagecsv.add(messageList[i])
                    }
                    Log.d("list", messagecsv.toString())

                    val smsAdapter = MessageAdapter(messagecsv, this@HomeActivity)
                    //recyclerView.adapter = null
                    recyclerView.adapter = smsAdapter
                    recyclerView.adapter?.notifyDataSetChanged()

                    traction.text = getString(R.string.credited_text) + String.format(
                        "%.0f",
                        messageDataResponse.creditedAmount
                    ) + getString(R.string.debited_text) + String.format(
                        "%.0f",
                        messageDataResponse.debittedAmount
                    )
                } else {
                    recyclerView.adapter = null
                    traction.text = ""
                }
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }

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

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }


}

