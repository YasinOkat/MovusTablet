package com.yasin.movustablet

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import com.yasin.movustablet.databinding.ActivityMainBinding
import com.yasin.movustablet.databinding.YeniAracDialogBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import okhttp3.*
import org.json.JSONException
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private var dialogBinding: YeniAracDialogBinding? = null
    private var customDialog: Dialog? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        dialogBinding = YeniAracDialogBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        mainScope.launch {
            try {
                val rampaOptions = fetchRampaOptions()
                setupRampaNoSpinner(rampaOptions)
                setupDaysNoSpinner()
                setupGecikmeNoktasiSpinner()
                setupGecikmeNedeniSpinner()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding?.btnYeniArac?.setOnClickListener {

            showCustomDialog()
        }

        binding?.btnRampaYanas?.setOnClickListener {
            sendRequest("http://192.168.1.193:5000/insertArac", "Araç Yanaşma Tamamlandı", "rampa_yanasma")
        }

        binding?.btnToplamaBasla?.setOnClickListener {
            sendRequest("http://192.168.1.193:5000/insertArac", "Toplama Başlatıldı", "toplama_baslama")
        }

        binding?.btnAracIciBasla?.setOnClickListener{
            sendRequest("http://192.168.1.193:5000/insertArac", "Yükleme Başlatıldı", "arac_ici_baslama")
        }

        binding?.btnToplamaBitis?.setOnClickListener{
            sendRequest("http://192.168.1.193:5000/insertArac", "Toplama Bitirildi", "toplama_bitis")
        }

        binding?.btnAracIciBitis?.setOnClickListener{
            sendRequest("http://192.168.1.193:5000/insertArac", "Yükleme Bitirildi", "yukleme_bitis")
        }

        binding?.btnRampaCikis?.setOnClickListener{
            sendRequest("http://192.168.1.193:5000/insertArac", "Araç Çıkış Tamamlandı", "rampa_cikis")
            setupDaysNoSpinner()
        }

        binding?.btnEkle?.setOnClickListener{
            val gecikmeNedeni = binding?.spinGecikmeNeden?.selectedItem.toString()
            sendRequest("http://192.168.1.193:5000/insertArac", "Gecikme Eklendi", gecikmeNedeni)
        }
    }



    private fun sendRequest(url: String, message: String, action: String) {
        val client = OkHttpClient()

        val daysNo = binding?.spinDays?.selectedItem.toString()

        val gecikmeDk = binding?.etGecikmeSuresi?.text.toString().toIntOrNull()
        val urunSayisi = binding?.etUrunSayisi?.text.toString()

        if (daysNo.isEmpty() || daysNo == "Days No Seçiniz") {
            Toast.makeText(this@MainActivity, "Days NO seçiniz", Toast.LENGTH_LONG).show()
            return
        }

        else {

            val json = JSONObject()
            json.put("days_no", daysNo)
            json.put("action", action)
            json.put("gecikme_dk", gecikmeDk)
            json.put("gecikme_adet", urunSayisi)

            val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Bir Hata Oluştu", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            })
        }
    }


    private fun showCustomDialog() {
        if (customDialog == null) {
            createCustomDialog()
        }

        customDialog?.show()
    }

    private fun createCustomDialog() {
        customDialog = Dialog(this)
        dialogBinding?.root?.let { customDialog?.setContentView(it) }

        mainScope.launch {
            try {
                val rampaOptions = fetchRampaOptions()
                setupRampaSpinner(rampaOptions)
                setupRampaGorevlisiSpinner()
                setupYuklemeci1Spinner()
                setupVardiyaAmiriSpinner()
                setupForkliftOperatoruSpinner()
                setupYuklemeci2Spinner()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        dialogBinding?.btnEkle?.setOnClickListener {
            val selectedYuklemeci = dialogBinding?.spinYuklemeci1?.selectedItem?.toString()
            val selectedYuklemeci2 = dialogBinding?.spinYuklemeci2?.selectedItem?.toString()
            val selectedVardiyaAmiri = dialogBinding?.spinVardiyaAmir?.selectedItem?.toString()
            val selectedRampaSorumlusu = dialogBinding?.spinRampaGorevlisi?.selectedItem.toString()
            val selectedRampa = dialogBinding?.spinRampaGorevlisi?.selectedItem.toString()
            val selectedForkliftOperatoru = dialogBinding?.spinForkliftOperatoru?.selectedItem.toString()

            val daysNo = dialogBinding?.etDays?.text.toString().toLongOrNull()
            val aracPlaka = dialogBinding?.etAracPlaka?.text.toString()
                .uppercase(Locale.getDefault()).filter { !it.isWhitespace() }
            val dorsePlaka = dialogBinding?.etDorsePlaka?.text.toString()
                .uppercase(Locale.getDefault()).filter { !it.isWhitespace() }
            val urunSayisi = dialogBinding?.etUrunSayisi?.text.toString().toIntOrNull()


            if (selectedYuklemeci != null && selectedYuklemeci != "Yüklemeci" &&
                selectedYuklemeci2 != null && selectedYuklemeci2 != "Yüklemeci" &&
                selectedVardiyaAmiri != null && selectedVardiyaAmiri != "Vardiya Amiri" &&
                selectedRampaSorumlusu != null && selectedRampaSorumlusu != "Rampa Görevlisi" &&
                selectedRampa != null && selectedRampa != "Rampa Görevlisi" &&
                selectedForkliftOperatoru != null && selectedForkliftOperatoru != "Forklift Operatörü" &&
                daysNo != null && daysNo > 0 && aracPlaka.isNotEmpty() && dorsePlaka.isNotEmpty() && urunSayisi != null
            ) {
                val url = "http://192.168.1.193:5000/insertLog"

                val json = """
                    {
                        "yuklemeci": "$selectedYuklemeci",
                        "yuklemeci2": "$selectedYuklemeci2",
                        "vardiya_amiri": "$selectedVardiyaAmiri",
                        "rampa_sorumlusu": "$selectedRampaSorumlusu",
                        "rampa_no": "$selectedRampa",
                        "forklift_operatoru": "$selectedForkliftOperatoru",
                        "days": "$daysNo",
                        "arac_plaka": "$aracPlaka",
                        "dorse_plaka": "$dorsePlaka",
                        "urun_sayisi": "$urunSayisi"
                    }
                """.trimIndent()

                val mediaType = "application/json".toMediaTypeOrNull()
                val requestBody = json.toRequestBody(mediaType)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val client = OkHttpClient()
                        val request = Request.Builder()
                            .url(url)
                            .post(requestBody)
                            .build()

                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_LONG).show()
                                customDialog?.dismiss()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                customDialog?.dismiss()

                setupDaysNoSpinner()
            }
            else{
                Toast.makeText(this, "Lütfen bütün kısımları giriniz", Toast.LENGTH_LONG).show()
            }
        }

        dialogBinding?.btnCancel?.setOnClickListener {
            customDialog?.dismiss()
        }

        customDialog?.setCanceledOnTouchOutside(false)
    }

    private suspend fun fetchRampaOptions(): List<String> = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://192.168.1.193:5000/getRampaOptions")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected response code: $response")

            val responseBody = response.body?.string()
            val rampaOptions = mutableListOf<String>()

            if (responseBody != null) {
                val jsonArray = JSONArray(responseBody)
                rampaOptions.add("Rampa Seçiniz")
                for (i in 0 until jsonArray.length()) {
                    val jsonObject: JSONObject = jsonArray.getJSONObject(i)
                    val rampaOption = jsonObject.getString("rampa")
                    rampaOptions.add(rampaOption)
                }
            }
            rampaOptions
        }
    }

    private suspend fun fetchPersoneller(gorev: String): List<String> = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val url = HttpUrl.Builder()
            .scheme("http")
            .host("192.168.1.193")
            .port(5000)
            .addPathSegment("getPersoneller")
            .addQueryParameter("gorev", gorev)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected response code: $response")

            val responseBody = response.body?.string()
            val personeller = mutableListOf<String>()

            if (responseBody != null) {
                val jsonArray = JSONArray(responseBody)
                personeller.add(gorev)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject: JSONObject = jsonArray.getJSONObject(i)
                    val isim = jsonObject.getString("isim")
                    val soyisim = jsonObject.getString("soyisim")
                    personeller.add("$isim $soyisim")
                }
            }
            personeller
        }
    }

    private fun setupRampaNoSpinner(rampaOptions: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, rampaOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding?.spinRampa?.adapter = adapter
    }

    private fun setupDaysNoSpinner() {
        val url = "http://192.168.1.193:5000/getDaysNo"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to retrieve days numbers", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()

                val daysNumbers = parseDaysNumbers(responseData)

                runOnUiThread {
                    val options = listOf("Days No Seçiniz") + daysNumbers
                    val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, options)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding?.spinDays?.adapter = adapter
                }
            }
        })
    }

    private fun parseDaysNumbers(responseData: String?): List<String> {
        val daysNumbers = mutableListOf<String>()

        try {
            val jsonArray = JSONArray(responseData)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val days = jsonObject.getString("days")
                daysNumbers.add(days)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return daysNumbers
    }


    private fun setupGecikmeNoktasiSpinner() {
        val options = listOf("Gecikme Noktası", "Toplama", "Yükleme")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding?.spinGecikmeNokta?.adapter = adapter
    }

    private fun setupGecikmeNedeniSpinner() {
        val options = listOf("Gecikme Nedeni", "Düşüm", "Bloke", "İlave", "Diğer")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding?.spinGecikmeNeden?.adapter = adapter
    }

    private suspend fun setupVardiyaAmiriSpinner() {
        val gorev = "Vardiya Amiri"
        val personeller = fetchPersoneller(gorev)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, personeller)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding?.spinVardiyaAmir?.adapter = adapter
    }

    private suspend fun setupRampaGorevlisiSpinner() {
        val gorev = "Rampa Sorumlusu"
        val personeller = fetchPersoneller(gorev)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, personeller)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding?.spinRampaGorevlisi?.adapter = adapter
    }

    private suspend fun setupYuklemeci1Spinner() {
        val gorev = "Yüklemeci"
        val personeller = fetchPersoneller(gorev)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, personeller)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding?.spinYuklemeci1?.adapter = adapter
    }

    private suspend fun setupYuklemeci2Spinner() {
        val gorev = "Yüklemeci"
        val personeller =fetchPersoneller(gorev)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, personeller)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding?.spinYuklemeci2?.adapter = adapter
    }

    private suspend fun setupForkliftOperatoruSpinner() {
        val gorev = "Forklift Operatörü"
        val personeller = fetchPersoneller(gorev)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, personeller)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding?.spinForkliftOperatoru?.adapter = adapter
    }

    private fun setupRampaSpinner(rampaOptions: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, rampaOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding?.spinRampa?.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
        dialogBinding = null
        customDialog?.dismiss()
        customDialog = null
        mainScope.cancel()
    }
}
