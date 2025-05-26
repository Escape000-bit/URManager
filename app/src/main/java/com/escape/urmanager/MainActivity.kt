package com.escape.urmanager
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlarmManager
import okhttp3.OkHttpClient
import android.Manifest
import okhttp3.Request
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import kotlin.random.Random
import android.provider.Settings
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.escape.urmanager.util.*
import android.security.keystore.*
import org.bouncycastle.asn1.ASN1Enumerated
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1TaggedObject
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.DLTaggedObject
import android.view.MenuInflater
import android.os.Bundle
import android.widget.Button
import android.widget.PopupMenu
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import java.nio.charset.Charset
import androidx.core.widget.doAfterTextChanged
import android.text.Editable
import android.text.TextWatcher
import androidx.cardview.widget.CardView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.edit
import java.io.InputStreamReader
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.ViewGroup
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.RelativeSizeSpan
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.OpenableColumns
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.InputType
import android.text.Layout
import android.text.Spannable
import android.text.method.ScrollingMovementMethod
import android.text.style.AlignmentSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.*
import android.widget.LinearLayout.LayoutParams
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.Space
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Switch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.view.marginTop
import com.airbnb.lottie.LottieAnimationView
import com.escape.urmanager.util.Downloader
import com.escape.urmanager.util.MagiskInstaller
import com.escape.urmanager.util.RootUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.internal.wait
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.HttpsURLConnection


fun saveLanguage(context: Context, language: String) {
    val file = File(context.filesDir, "language.json")
    file.writeText(Gson().toJson(mapOf("language" to language)))

}

// **2. Sprache aus JSON laden**
fun loadLanguage(context: Context): String {
    val file = File(context.filesDir, "language.json")
    return if (file.exists()) {
        val json = file.readText()
        Gson().fromJson(json, Map::class.java)["language"] as? String ?: "English"
    } else {
        "English" // Standard: Englisch
    }
}

// **3. Übersetzungen aus `assets/translations.json` laden**
fun loadTranslations(context: Context): Map<String, Map<String, String>> {
    val json = context.assets.open("translations.json").bufferedReader().use { it.readText() }
    return Gson().fromJson(json, Map::class.java) as Map<String, Map<String, String>>
}

class MainActivity : ComponentActivity() {
    private val allmagiskModules = mutableListOf<String>()
    private var loadedCount = 0
    private val loadBatchSize = 5

    private var allApps: List<ApplicationInfo> = emptyList()
    private var currentLoadedCount = 0

    private var magiskDenylist: Set<String> = emptySet()

    val CURRENT_VERSION = 1.3


    private lateinit var statusText: TextView
    private lateinit var appListLayout: LinearLayout
    private lateinit var appListLayoutTricky: LinearLayout
    private lateinit var homeContainer: LinearLayout
    private lateinit var appListContainer: LinearLayout
    private lateinit var settingsContainer: ScrollView
    private lateinit var modulesContainer: ScrollView
    private lateinit var module_containerr: LinearLayout
    private lateinit var magisk_patch_container: LinearLayout
    private lateinit var denylist_containerr: LinearLayout
    private lateinit var moduleContainer: LinearLayout
    private lateinit var lsContainer: LinearLayout
    private lateinit var outputText: TextView
    private lateinit var BackupContainer: LinearLayout
    private lateinit var languagecontainer: LinearLayout
    private lateinit var selectedBootImg: File

    lateinit var languagescroll: ScrollView
    lateinit var buttononhome: LinearLayout
    lateinit var tricky_store_container: LinearLayout

    private lateinit var magisk_patch_select: String

    data class lsModuleInfo(
        val title: String,
        val description: String,
        val homepageUrl: String,
        val downloadurl: String,
        val apiUrl: String
    )
    private var allModules = mutableListOf<lsModuleInfo>()
    val filteredModules = mutableListOf<lsModuleInfo>()

    private lateinit var scrollView: ScrollView
    private lateinit var container: LinearLayout
    private var page = 0
    private val pageSize = 20
    private var loading = false

    private val REQUEST_CODE_WRITE_STORAGE = 1001

    private val sharedPrefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    private var showUserApps = true
    private var showSystemApps = false

    private var showUserAppsBackup = true
    private var showSystemAppsBackup = false

    private var showUserAppsDenylist = true
    private var showSystemAppsDenylist = false

    private var showUserAppsTrickyStore = true
    private var showSystemAppsTrickyStore = false

    private val selectedPackages = mutableSetOf<String>()
    private val selectedPackagesBackup = mutableSetOf<String>()

    private val scope = CoroutineScope(Dispatchers.Main)

    private val containerBackStack = mutableListOf<View>()

    // Wichtige System-Apps, die du festlegst
    private val importantSystemApps = listOf(
        "com.android.vending", // Google Play Store
        "com.google.android.gms" // Google Play Dienste
        // Weitere wichtige Apps kannst du hier hinzufügen
    )


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Initialisierung der UI-Elemente
        statusText = findViewById(R.id.status_text)
        appListLayout = findViewById(R.id.app_list_layout)

        homeContainer = findViewById(R.id.home_container)
        appListContainer = findViewById(R.id.app_list_container)
        settingsContainer = findViewById(R.id.settings_container)
        modulesContainer = findViewById(R.id.modules_container)
        moduleContainer = findViewById<LinearLayout>(R.id.module_container_page)
        lsContainer = findViewById<LinearLayout>(R.id.lsposed_container)
        languagescroll = findViewById(R.id.langague_scroll)
        languagecontainer = findViewById(R.id.langague_container)
        tricky_store_container = findViewById(R.id.tricky_store_container)
        appListLayoutTricky = findViewById(R.id.app_list_layout_tricky)
        module_containerr = findViewById(R.id.module_containerr)
        denylist_containerr = findViewById(R.id.denylist_layout_container)
        magisk_patch_container = findViewById(R.id.magisk_patch)
        BackupContainer = findViewById(R.id.backup_container)
        containerBackStack.add(homeContainer)

        scrollView = findViewById(R.id.scrollView)
        container = findViewById(R.id.container)

        val selectedLanguage = loadLanguage(this)
        val translations = loadTranslations(this)
        val texts = translations[selectedLanguage] ?: emptyMap()

        val spinner: Spinner = findViewById(R.id.imageSelector)
        val items = listOf("Magisk Offical", "Magisk Alpha", "Magisk Canery", "Magisk Kitsune")

        val adapter = ArrayAdapter(this, R.layout.spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val bottomReached = scrollView.getChildAt(0).bottom <= scrollView.scrollY + scrollView.height + 200
            if (bottomReached && !loading) {
                loadNextPage()
            }
        }

        val button: Button = findViewById(R.id.test)
        button.setOnClickListener {
            copyAssetToPath(this, "get_extra.sh", "/data/adb/get_extra.sh")
            showAutoConfigPopup(
                context = this,
            )
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = items[position]
                magisk_patch_select = selected
                copyAssetsToSdcard(selected) {

                }
                Toast.makeText(this@MainActivity, "Selected: $selected", Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val installIntegrityButton = findViewById<Button>(R.id.install_integrity_button)
        val fixWhatsappButton = findViewById<Button>(R.id.fix_whatsapp_button)
        val appListButton = findViewById<Button>(R.id.app_list_button)
        val appbackupButton = findViewById<Button>(R.id.backup_button)
        val settingsButton = findViewById<ImageButton>(R.id.settings_button)
        val clear_play_serviceButton = findViewById<Button>(R.id.clear_play_service_button)
        val HomeButtonBackup = findViewById<ImageButton>(R.id.home_button_backup)
        val open_lsposed_button = findViewById<Button>(R.id.lsposed_repo)
        val open_language_button = findViewById<Button>(R.id.selectLangage)
        val open_tricky_addon_container_button = findViewById<Button>(R.id.tricky_store_addon_button)
        val modules_containerr = findViewById<LinearLayout>(R.id.module_containerr)
        val backup_app_button = findViewById<Button>(R.id.bakcup_selected_apps)
        val open_module_containerr = findViewById<ImageButton>(R.id.open_button_modules)
        val open_denylist_containerr = findViewById<ImageButton>(R.id.open_magisk_denylist)
        val open_patch_container = findViewById<ImageButton>(R.id.open_magisk_patch)

        val setboothashButton = findViewById<Button>(R.id.setboothash)

        val modulesButton: Button = findViewById(R.id.installed_modules_page)
        val homeButtonAppList: ImageButton = findViewById(R.id.home_button_app_list)
        val homeButtonModules: ImageButton = findViewById(R.id.home_button_modules)
        val homeButtonlsrepo: ImageButton = findViewById(R.id.home_button_lsposed_repo)
        val homeButtonTrickyStore: ImageButton = findViewById(R.id.home_button_tricky)
        val buttononhomee: LinearLayout = findViewById(R.id.buttononhome)
        // Deine beiden CheckBoxen holen

        val rootStatusLabel = findViewById<TextView>(R.id.root_status_label)
        val rootStatusImage = findViewById<ImageView>(R.id.root_status_icon)
        val rootlayout = findViewById<LinearLayout>(R.id.root_status_container)

        val selectBootBtn = findViewById<Button>(R.id.selectImage)
        val patchBtn = findViewById<Button>(R.id.startPatch)
        outputText = findViewById(R.id.outputTextView)


        selectBootBtn.setOnClickListener {
            pickBootImage()
        }

        patchBtn.setOnClickListener {
            runBootPatchScript()
        }

        setbuttonstexts()

        val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris != null && uris.isNotEmpty()) {
                installModulesFromUris(uris)
            }
        }

        val moreButton : ImageButton = findViewById(R.id.three_dots_menu)
        val moreButtonTricky : ImageButton = findViewById(R.id.three_dots_menu_tricky)
        val moreButtonDenylist : ImageButton = findViewById(R.id.three_dots_menu_denylist)
        val moreButtonBackup : ImageButton = findViewById(R.id.three_dots_menu_backup)

        val checkChangedListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            val list = findViewById<LinearLayout>(R.id.app_list_layout)

            when (buttonView.id) {
                R.id.user_apps_checkbox -> showUserApps = isChecked
                R.id.system_apps_checkbox -> showSystemApps = isChecked
            }
            loadInstalledApps(list) // Liste neu laden
        }

        val checkChangedListenerBackup = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            val list = findViewById<LinearLayout>(R.id.app_list_layout_backup)

            when (buttonView.id) {
                R.id.user_apps_checkbox -> showUserAppsBackup = isChecked
                R.id.system_apps_checkbox -> showSystemAppsBackup = isChecked
            }
            loadInstalledAppsBackup(list) // Liste neu laden
        }

        val checkChangedListenerDenylist = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            val denylist_layout = findViewById<LinearLayout>(R.id.denylist_list_layout)

            when (buttonView.id) {
                R.id.user_apps_checkbox -> showUserAppsDenylist = isChecked
                R.id.system_apps_checkbox -> showSystemAppsDenylist = isChecked
            }
            loadInstalledAppsDenylist(denylist_layout) // Liste neu laden
        }

        val checkChangedListenerTrickyStore = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            val list2 = findViewById<LinearLayout>(R.id.app_list_layout_tricky)
            when (buttonView.id) {
                R.id.user_apps_checkbox -> showUserAppsTrickyStore = isChecked
                R.id.system_apps_checkbox -> showSystemAppsTrickyStore = isChecked
            }
            loadInstalledAppsTrickyStore(list2) // Liste neu laden
        }

        moreButtonBackup.setOnClickListener {
            val inflater = LayoutInflater.from(this)
            val popupView = inflater.inflate(R.layout.popup_menu_backup, null)

            val popupWindow = PopupWindow(
                popupView,
                WRAP_CONTENT,
                WRAP_CONTENT,
                true
            )

            val userAppsCheckbox = popupView.findViewById<CheckBox>(R.id.user_apps_checkbox)
            val systemAppsCheckbox = popupView.findViewById<CheckBox>(R.id.system_apps_checkbox)

            // Vorherige Zustände setzen (optional)
            userAppsCheckbox.isChecked = showUserAppsBackup
            systemAppsCheckbox.isChecked = showSystemAppsBackup

            userAppsCheckbox.setOnCheckedChangeListener(checkChangedListenerBackup)
            systemAppsCheckbox.setOnCheckedChangeListener(checkChangedListenerBackup)

            popupWindow.elevation = 10f
            popupWindow.showAsDropDown(moreButtonBackup)
        }

        moreButtonTricky.setOnClickListener {
            val inflater = LayoutInflater.from(this)
            val popupView = inflater.inflate(R.layout.popup_menu_tricky, null)

            val popupWindow = PopupWindow(
                popupView,
                WRAP_CONTENT,
                WRAP_CONTENT,
                true
            )

            val userAppsCheckbox = popupView.findViewById<CheckBox>(R.id.user_apps_checkbox)
            val systemAppsCheckbox = popupView.findViewById<CheckBox>(R.id.system_apps_checkbox)

            // Vorherige Zustände setzen (optional)
            userAppsCheckbox.isChecked = showUserAppsTrickyStore
            systemAppsCheckbox.isChecked = showSystemAppsTrickyStore

            userAppsCheckbox.setOnCheckedChangeListener(checkChangedListenerTrickyStore)
            systemAppsCheckbox.setOnCheckedChangeListener(checkChangedListenerTrickyStore)

            popupWindow.elevation = 10f
            popupWindow.showAsDropDown(moreButtonTricky)
        }

        moreButton.setOnClickListener {
            val inflater = LayoutInflater.from(this)
            val popupView = inflater.inflate(R.layout.popup_menu, null)

            val popupWindow = PopupWindow(
                popupView,
                WRAP_CONTENT,
                WRAP_CONTENT,
                true
            )

            val userAppsCheckbox = popupView.findViewById<CheckBox>(R.id.user_apps_checkbox)
            val systemAppsCheckbox = popupView.findViewById<CheckBox>(R.id.system_apps_checkbox)

            // Vorherige Zustände setzen (optional)
            userAppsCheckbox.isChecked = showUserApps
            systemAppsCheckbox.isChecked = showSystemApps

            userAppsCheckbox.setOnCheckedChangeListener(checkChangedListener)
            systemAppsCheckbox.setOnCheckedChangeListener(checkChangedListener)

            popupWindow.elevation = 10f
            popupWindow.showAsDropDown(moreButton)
        }

        moreButtonDenylist.setOnClickListener {
            val inflater = LayoutInflater.from(this)
            val popupView = inflater.inflate(R.layout.popup_menu_denylist, null)

            val popupWindow = PopupWindow(
                popupView,
                WRAP_CONTENT,
                WRAP_CONTENT,
                true
            )

            val userAppsCheckbox = popupView.findViewById<CheckBox>(R.id.user_apps_checkbox)
            val systemAppsCheckbox = popupView.findViewById<CheckBox>(R.id.system_apps_checkbox)

            // Vorherige Zustände setzen (optional)
            userAppsCheckbox.isChecked = showUserAppsDenylist
            systemAppsCheckbox.isChecked = showSystemAppsDenylist

            userAppsCheckbox.setOnCheckedChangeListener(checkChangedListenerDenylist)
            systemAppsCheckbox.setOnCheckedChangeListener(checkChangedListenerDenylist)

            popupWindow.elevation = 10f
            popupWindow.showAsDropDown(moreButtonDenylist)
        }
        findViewById<Button>(R.id.btn_select_modules).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            filePickerLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
        }

        val btnHideMagisk = findViewById<Button>(R.id.btnHideMagisk)

        btnHideMagisk.setOnClickListener {
            hideMagiskAndGrantRoot()
        }

        val allButtons = listOf(
            installIntegrityButton,
            appListButton,
            fixWhatsappButton,
            setboothashButton,
            settingsButton,
            clear_play_serviceButton,
            modulesButton,
            open_lsposed_button
        )

        val delmenubutton = findViewById<ImageButton>(R.id.deleteButton)

        fun getCurrentBootHash(): String {
            return try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "getprop ro.boot.vbmeta.digest"))
                val output = process.inputStream.bufferedReader().readText().trim()
                process.waitFor()
                output // kann leer sein, aber nie null
            } catch (e: Exception) {
                "" // Fehler → leeren String zurückgeben
            }
        }

        val result = getVerifiedBootHash()
        val currentHash = getCurrentBootHash()

        if (currentHash != result) {
            setboothashButton.visibility = View.VISIBLE
        } else {
            setboothashButton.visibility = View.GONE
        }


        UpdateHelper.checkForUpdate(this) { updateAvailable ->
            if (updateAvailable) {
                // z. B. Button sichtbar machen oder anderes Verhalten
                runOnUiThread {
                    val appIcon = findViewById<ImageView>(R.id.appicon)
                    val URManager = findViewById<TextView>(R.id.URManager)
                    val animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
                    appIcon.setImageResource(R.drawable.update_icon)
                    URManager.text = "Update"
                    appIcon.startAnimation(animation)
                    URManager.setOnClickListener{
                        showUpdateDialog(this)
                    }
                }
            } else {
                val appIcon = findViewById<ImageView>(R.id.appicon)
                val URManager = findViewById<TextView>(R.id.URManager)
                val animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
                runOnUiThread {
                    appIcon.setImageResource(R.drawable.ic_launcher_round)
                    URManager.text = "URManager"
                    appIcon.startAnimation(animation)
                    URManager.setOnClickListener{
                        Toast.makeText(this, "App is Up-to-date", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        fun showBootHashPopup(context: Context, result: String, onSuccess: () -> Unit) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_boot_hash, null)
            val content = dialogView.findViewById<TextView>(R.id.dialogContent)
            val progress = dialogView.findViewById<ProgressBar>(R.id.dialogProgress)
            val button = dialogView.findViewById<Button>(R.id.dialogPositiveButton)

            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            dialog.show()

            content.text = "${texts["BootHash"]}:\n$result"

            Handler(Looper.getMainLooper()).postDelayed({
                progress.visibility = View.VISIBLE
                writeBootHashScript(result)
                val newDigest = getCurrentBootHash()
                val success = newDigest == result

                (context as Activity).runOnUiThread {
                    progress.visibility = View.GONE
                    button.visibility = View.VISIBLE

                    if (success) {
                        content.text = "✅ ${texts["erfolg"]}"
                        button.text = "OK"
                        button.setOnClickListener {
                            dialog.dismiss()
                            onSuccess()
                        }
                    } else {
                        content.text = "❌ Failed\n\nExpected:\n$result\n\nFound:\n$newDigest"
                        button.text = texts["close"]
                        button.setOnClickListener { dialog.dismiss() }
                    }
                }
            }, 300)
        }

        setboothashButton.setOnClickListener {
            showBootHashPopup(this, getVerifiedBootHash()) {
                setboothashButton.visibility = View.GONE
            }
        }

        delmenubutton.setOnClickListener {

            val selectedLanguage = loadLanguage(this)
            val translations = loadTranslations(this)
            val texts = translations[selectedLanguage] ?: emptyMap()

            val popup = PopupMenu(this, delmenubutton)
            popup.menuInflater.inflate(R.menu.delete_menu, popup.menu)

            // Menüpunkt auswählen
            popup.menu.findItem(R.id.clear_button).title = "${texts["clear_button"]} (${selectedPackages.size})"
            popup.menu.findItem(R.id.clear_important_button).title = "${texts["clear_important_button"]} (${selectedPackages.size})"
            popup.menu.findItem(R.id.dell_selected_apps).title = "${texts["del3"]} (${selectedPackages.size})"


            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.clear_button -> {
                        val results = StringBuilder()
                        selectedPackages.forEach { packageName ->
                            val result = clearAppData(packageName)
                            if (result) {
                                results.append("$packageName ${texts["del"] ?: "deleted"}\n")
                                Toast.makeText(this, "$packageName ${texts["del"] ?: "deleted"}", Toast.LENGTH_SHORT).show()
                            } else {
                                results.append("$packageName ${texts["nodel"] ?: "not deleted"}\n")
                                Toast.makeText(this, "$packageName ${texts["nodel"] ?: "not deleted"}", Toast.LENGTH_SHORT).show()
                            }
                        }

                        statusText.text = results.toString()
                        true
                    }

                    R.id.clear_important_button -> {
                        val results = importantSystemApps.map { packageName ->
                            val result = clearAppData(packageName)
                            if (result) {
                                Toast.makeText(this, "$packageName ${texts["del"] ?: "deleted"}", Toast.LENGTH_SHORT).show()
                            }
                            else {
                                Toast.makeText(this, "$packageName ${texts["nodel"] ?: "not deleted"}", Toast.LENGTH_SHORT).show()
                            }
                        }.joinToString("\n")

                        true
                    }
                    R.id.dell_selected_apps -> {
                        val inflater = LayoutInflater.from(this)
                        val dialogView = inflater.inflate(R.layout.progress_dialog, null)
                        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
                        val progressText = dialogView.findViewById<TextView>(R.id.progressText)

                        progressBar.max = selectedPackages.size
                        progressBar.progress = 0

                        val dialog = AlertDialog.Builder(this)
                            .setTitle(texts["deleting"] ?: "delete Apps")
                            .setView(dialogView)
                            .setCancelable(false)
                            .create()

                        dialog.show()

                        CoroutineScope(Dispatchers.IO).launch {
                            val results = StringBuilder()
                            selectedPackages.forEachIndexed { index, packageName ->
                                val process = Runtime.getRuntime().exec("su -c pm uninstall $packageName")
                                val exitCode = process.waitFor()

                                withContext(Dispatchers.Main) {
                                    progressBar.progress = index + 1
                                    progressText.text = "${texts["deleting"] ?: "Lösche"}: $packageName"
                                }

                                results.append(
                                    if (exitCode == 0)
                                        "$packageName ${texts["del"] ?: "gelöscht"}\n"
                                    else
                                        "$packageName ${texts["nodel"] ?: "nicht gelöscht"}\n"
                                )

                                delay(300) // optional: langsamer anzeigen
                            }

                            withContext(Dispatchers.Main) {
                                dialog.dismiss()
                                statusText.text = results.toString()
                                Toast.makeText(this@MainActivity, texts["done"] ?: "Fertig", Toast.LENGTH_SHORT).show()
                            }
                        }
                        true
                    }
                    R.id.make_selected_system_apps -> {
                        // Schritt 1: Erstelle einen Dialog zur Auswahl
                        val options = arrayOf(texts["systemapp"], texts["userapp"])

                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Select")
                            .setItems(options) { _, which ->
                                when (which) {
                                    0 -> { // "System-App"
                                        showProgressDialog(selectedPackages.toList(), ::makeSystemApp)
                                    }
                                    1 -> { // "Benutzer-App"
                                        showProgressDialog(selectedPackages.toList(), ::makeUserApp)
                                    }
                                }
                            }
                            .setCancelable(false)
                            .show()

                        true
                    }
                    else -> false
                }
            }
            popup.show() // Zeige das Menü an
        }

        fun hasRootAccess(): Boolean {
            return try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo RootCheck"))
                val result = process.inputStream.bufferedReader().readLine()
                result == "RootCheck"
            } catch (e: Exception) {
                false
            }
        }

        rootlayout.setOnClickListener {
            if (!hasRootAccess()) {
                Toast.makeText(this, texts["noroot"] ?:"No Root", Toast.LENGTH_LONG).show()
                allButtons.forEach {
                    it.isEnabled = false
                    it.alpha = 0.5f // optional: optisch als "deaktiviert" markieren
                }
                rootStatusLabel.text = "❌ ${texts["noroot"] ?: "No Root"}"
                rootStatusLabel.setTextColor(Color.parseColor("#F44336")) // Rot
                rootStatusImage.setImageResource(R.drawable.remove)
            } else {
                allButtons.forEach {
                    it.isEnabled = true
                    it.alpha = 1f
                }
                Toast.makeText(this, texts["yesroot"] ?:"No Root", Toast.LENGTH_SHORT).show()
                rootStatusLabel.text = "✅ ${texts["yesroot"] ?:"No Root"}"
                rootStatusLabel.setTextColor(Color.parseColor("#4CAF50")) // Grün
                rootStatusImage.setImageResource(R.drawable.baseline_verified_24)

            }

        }

        var lastRootStatus: Boolean? = null

        fun fadeIn(view: View) {
            val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
            fadeIn.duration = 1000 // 500 ms für den Fade-Effekt
            fadeIn.start()
        }

        fun fadeOut(view: View) {
            val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
            fadeOut.duration = 1000
            fadeOut.start()
        }

        if (!hasRootAccess()) {
            allButtons.forEach {
                it.isEnabled = false
                it.alpha = 0.5f // optional: optisch als "deaktiviert" markieren
            }
            rootStatusLabel.text = "❌ ${texts["noroot"] ?: "No Root"}"
            rootStatusLabel.setTextColor(Color.parseColor("#F44336")) // Rot
            rootStatusImage.setImageResource(R.drawable.remove)
        } else {
            Toast.makeText(this, "Root-Zugriff erkannt", Toast.LENGTH_SHORT).show()
            rootStatusLabel.text = "✅ ${texts["yesroot"] ?:"No Root"}"
            rootStatusLabel.setTextColor(Color.parseColor("#4CAF50")) // Grün
            rootStatusImage.setImageResource(R.drawable.baseline_verified_24)
        }

        // Listener setzen

        // An beide Checkboxen den Listener hängen

        val searchBox = findViewById<EditText>(R.id.search_box)
        val searchBoxTricky = findViewById<EditText>(R.id.search_box_tricky)
        val searchBoxDenylist = findViewById<EditText>(R.id.search_box_denylist)
        val searchBoxlsposed = findViewById<EditText>(R.id.search_box_lsposed)
        val searchBoxBackup = findViewById<EditText>(R.id.search_box_backup)

        var searchJob: Job? = null
        var searchJoblsposed: Job? = null
        var searchJobdenylist: Job? = null
        var searchJobbackup: Job? = null

        searchBoxBackup.addTextChangedListener(object : TextWatcher {
            val list = findViewById<LinearLayout>(R.id.app_list_layout_backup)
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""

                // Vorherige Suche abbrechen
                searchJobbackup?.cancel()

                // Neue Suche starten mit Verzögerung
                searchJobbackup = lifecycleScope.launch {
                    delay(500) // 300 ms warten
                    loadInstalledAppsBackup(list, query)
                }
            }
        })

        searchBox.addTextChangedListener(object : TextWatcher {
            val list = findViewById<LinearLayout>(R.id.app_list_layout)
            val list2 = findViewById<LinearLayout>(R.id.app_list_layout_tricky)
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""

                // Vorherige Suche abbrechen
                searchJob?.cancel()

                // Neue Suche starten mit Verzögerung
                searchJob = lifecycleScope.launch {
                    delay(500) // 300 ms warten
                    loadInstalledApps(list, query)
                }
            }
        })

        searchBoxDenylist.addTextChangedListener(object : TextWatcher {
            val denylist_layout = findViewById<LinearLayout>(R.id.denylist_list_layout)

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""

                // Vorherige Suche abbrechen
                searchJobdenylist?.cancel()

                // Neue Suche starten mit Verzögerung
                searchJobdenylist = lifecycleScope.launch {
                    delay(500) // 300 ms warten
                    loadInstalledAppsDenylist(denylist_layout, query)
                }
            }
        })

        searchBoxTricky.addTextChangedListener(object : TextWatcher {
            val list2 = findViewById<LinearLayout>(R.id.app_list_layout_tricky)
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""

                // Vorherige Suche abbrechen
                searchJob?.cancel()

                // Neue Suche starten mit Verzögerung
                searchJob = lifecycleScope.launch {
                    delay(500) // 300 ms warten
                    loadInstalledAppsTrickyStore(list2, query)
                }
            }
        })

        try {
            val process = ProcessBuilder(
                "su", "-c", "if [ -d /data/adb/modules/tricky_store ]; then echo exists; else echo missing; fi"
            ).redirectErrorStream(true).start()

            val result = process.inputStream.bufferedReader().readText().trim()

            if (result == "exists") {
                open_tricky_addon_container_button.visibility = View.VISIBLE
            } else {
                open_tricky_addon_container_button.visibility = View.GONE
            }
        } catch (e: Exception) {
            open_tricky_addon_container_button.visibility = View.GONE
            e.printStackTrace()
        }


        searchBoxlsposed.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""

                // Vorherige Suche abbrechen
                searchJoblsposed?.cancel()

                // Neue Suche starten mit Verzögerung
                searchJoblsposed = lifecycleScope.launch {
                    delay(500) // 300 ms warten
                    fetchModulesFromWeb(query)                }
            }
        })

        val buttonContainerMap = mapOf(
            appListButton to appListContainer,
            open_lsposed_button to lsContainer,
            settingsButton to settingsContainer,
            modulesButton to moduleContainer,
            HomeButtonBackup to homeContainer,
            homeButtonAppList to homeContainer,
            appbackupButton to BackupContainer,
            homeButtonlsrepo to homeContainer,
            homeButtonTrickyStore to homeContainer,
            homeButtonModules to homeContainer,
            open_language_button to languagescroll,
            open_tricky_addon_container_button to tricky_store_container
        )

        open_module_containerr.setOnClickListener {
            denylist_containerr.visibility = View.GONE
            magisk_patch_container.visibility = View.GONE
            module_containerr.visibility = View.VISIBLE
            loadMagiskModulesWithRoot()
        }

        open_patch_container.setOnClickListener {
            denylist_containerr.visibility = View.GONE
            module_containerr.visibility = View.GONE
            magisk_patch_container.visibility = View.VISIBLE
            loadMagiskModulesWithRoot()
        }

        open_denylist_containerr.setOnClickListener {
            val deylist_layout = findViewById<LinearLayout>(R.id.denylist_list_layout)
            module_containerr.visibility = View.GONE
            magisk_patch_container.visibility = View.GONE
            denylist_containerr.visibility = View.VISIBLE
            loadInstalledAppsDenylist(deylist_layout)
        }

        val list = findViewById<LinearLayout>(R.id.app_list_layout)
        val backuplist = findViewById<LinearLayout>(R.id.app_list_layout_backup)
        val list2 = findViewById<LinearLayout>(R.id.app_list_layout_tricky)

        val extraActions = mapOf(
            open_lsposed_button to { fetchModulesFromWeb() },
            modulesButton to { loadMagiskModulesWithRoot() },
            open_tricky_addon_container_button to { loadInstalledAppsTrickyStore(list2) },
            appListButton to { loadInstalledApps(list) },
            appbackupButton to { loadInstalledAppsBackup(backuplist) },
            homeButtonAppList to { loadInstalledApps(list) },
            appListButton to { loadInstalledAppsBackup(list) },
            open_language_button to { loadLanguagesFromAssets() },
            homeButtonlsrepo to { fetchModulesFromWeb() },
            settingsButton to {
                val hashText = findViewById<TextView>(R.id.boothash)
                val result = getVerifiedBootHash()
                hashText.text = "${texts["BootHash"]} \n$result"
                hashText.setOnClickListener{
                    writeBootHashScript(result)
                }
            }
        )

// Setze die Listener in einer Schleife
        buttonContainerMap.forEach { (button, container) ->
            button.setOnClickListener {
                showSection(container)
                extraActions[button]?.invoke()
            }
        }

        clear_play_serviceButton.setOnClickListener {
            val results = importantSystemApps.map { packageName ->
                val result = clearAppData(packageName)
                if (result) {
                    Toast.makeText(this, "$packageName ${texts["del"] ?: "deleted"}", Toast.LENGTH_SHORT).show()
                }
                else {
                    Toast.makeText(this, "$packageName ${texts["nodel"] ?: "not deleted"}", Toast.LENGTH_SHORT).show()
                }
            }.joinToString("\n")
        }

        fun isZygiskLoaded(): Boolean {
            try {
                // PID von zygote64 holen
                val pid = Runtime.getRuntime().exec(arrayOf("sh", "-c", "pidof zygote64")).inputStream
                    .bufferedReader().readLine()?.trim() ?: return false

                // Speicherabbild lesen und nach "zygisk" filtern
                val mapsPath = "/proc/$pid/maps"
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "cat $mapsPath | grep zygisk"))
                val output = process.inputStream.bufferedReader().readText()

                return output.contains("zygisk", ignoreCase = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false
        }

        backup_app_button.setOnClickListener {
            Thread {
                for (packageName in selectedPackagesBackup) {
                    val backupDir = "/sdcard/URBackup/$packageName"
                    try {
                        // Verzeichnis anlegen
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p $backupDir")).waitFor()

                        // 1. APKs sichern
                        val apkPaths = Runtime.getRuntime()
                            .exec(arrayOf("su", "-c", "pm path $packageName"))
                            .inputStream.bufferedReader().readLines()
                            .map { it.removePrefix("package:") }

                        apkPaths.forEachIndexed { index, apkPath ->
                            val apkName = if (index == 0) "base.apk" else "split$index.apk"
                            Runtime.getRuntime().exec(arrayOf("su", "-c", "cp \"$apkPath\" \"$backupDir/$apkName\"")).waitFor()
                        }

                        runOnUiThread {
                            Toast.makeText(this, "✅ sicher app daten", Toast.LENGTH_SHORT).show()
                        }

                        Runtime.getRuntime().exec("su -c 'tar -cf $backupDir/data.tar -C /data/data $packageName'").waitFor()

                        runOnUiThread {
                            Toast.makeText(this, "✅ sicher app Media", Toast.LENGTH_SHORT).show()
                        }
                        // 3. Media
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "[ -d /sdcard/Android/media/$packageName ] && cp -r /sdcard/Android/media/$packageName \"$backupDir/media\"")).waitFor()
                        runOnUiThread {
                            Toast.makeText(this, "✅ sicher app OBB", Toast.LENGTH_SHORT).show()
                        }
                        // 4. OBB
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "[ -d /sdcard/Android/obb/$packageName ] && cp -r /sdcard/Android/obb/$packageName \"$backupDir/obb\"")).waitFor()

                        runOnUiThread {
                            Toast.makeText(this, "✅ Backup fertig: $packageName", Toast.LENGTH_SHORT).show()
                        }

                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this, "❌ Fehler bei $packageName: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }.start()
        }

        fixWhatsappButton.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.whatsapp_fix_dialog, null)
            val dialogText = dialogView.findViewById<TextView>(R.id.dialogText)
            val rebootButton = dialogView.findViewById<Button>(R.id.cancelButton)
            val continueButton = dialogView.findViewById<Button>(R.id.continueButton)

            rebootButton.isEnabled = false
            rebootButton.alpha = 0.5f

            val dialog = AlertDialog.Builder(this, R.style.DialogNoCorners).apply {
                setView(dialogView)
                setCancelable(false)
            }.create()

            rebootButton.setOnClickListener { RootUtils.runCommand("reboot") }

            dialog.show() // Dialog anzeigen!

            Thread {
                // Prüfen, ob WhatsApp installiert ist
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm list packages | grep com.whatsapp"))
                val output = process.inputStream.bufferedReader().readText().trim()
                process.waitFor()

                runOnUiThread {
                    if (output.contains("package:com.whatsapp")) {
                        showWhatsappInstalledDialog(dialogText, rebootButton, continueButton, dialog)
                    } else {
                        fixwhatsapp(dialogText, rebootButton, continueButton, dialog)
                    }
                }
            }.start()
        }


        installIntegrityButton.setOnClickListener {
            val installFiles = mutableListOf<String>()
            val folder1 = File("/sdcard/Download/FixIntegrity")
            if (!folder1.exists()) {
                folder1.mkdirs()
            }


            val dialogBuilder = AlertDialog.Builder(this, R.style.DialogNoCorners)


            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.install_integrity_dialog, null)

            val dialogText = dialogView.findViewById<TextView>(R.id.dialogText)



            val scrollView = dialogView.findViewById<ScrollView>(R.id.scroll_view)

            val CloseButton = dialogView.findViewById<Button>(R.id.close_button)
            val RestartButton = dialogView.findViewById<Button>(R.id.reboot_button)
            val CleanButton = dialogView.findViewById<Button>(R.id.clean_button)

            val Buttons = listOf(CloseButton, CleanButton, RestartButton)

            for (button in Buttons) {
                button.isEnabled = false
                button.alpha = 0.25f
            }

            RestartButton.text = texts["restart"]
            CleanButton.text = texts["delplayserviceAndrestart"]
            CloseButton.text = texts["close"]

            dialogBuilder.setView(dialogView)
            dialogBuilder.setCancelable(false)

            val dialog = dialogBuilder.create()

            val displayMetrics = resources.displayMetrics
            val width = displayMetrics.widthPixels  // volle Displaybreite
            val height = displayMetrics.heightPixels / 2  // feste Höhe in px



            CloseButton.setOnClickListener { dialog.dismiss() }

            RestartButton.setOnClickListener {
                val success = RootUtils.runCommand("reboot") == 0

                if (success) {
                    dialogText.append("${texts["restart"]} ${texts["erfolg"]}")
                } else {
                    dialogText.append("can not reboot")
                }
            }

            CleanButton.setOnClickListener {
                Thread{
                    val Play = RootUtils.runCommand("pm clear com.android.vending") == 0
                    runOnUiThread {
                        if (Play) {
                            dialogText.append("${texts["del"]}: com.android.vending")
                        } else {
                            dialogText.append("Error")
                        }
                    }
                    val Service = RootUtils.runCommand("pm clear com.google.android.gms") == 0
                    runOnUiThread {
                        if (Service) {
                            dialogText.append("${texts["del"]}: com.google.android.gms")
                        } else {
                            dialogText.append("Error")
                        }
                    }
                }.start()

            }

            dialog.show()

            dialog.window?.setLayout(width, height)

            val folder = File("/sdcard/Download/FixIntegrity")
            if (!folder.exists()) {
                folder.mkdirs()
            }

            Thread {
                try {
                    val processes = Runtime.getRuntime().exec(arrayOf("su", "-c", "getprop ro.crypto.state"))
                    val resultt = processes.inputStream.bufferedReader().readText().trim()
                    processes.waitFor()

                    if (resultt == "unencrypted") {
                        runOnUiThread {
                            val spannable = SpannableHelper.colorText(Color.RED, "\n${texts["dataNotEncrypted"] ?: "Data is not encrypted – Spoofing..."}\n")
                            dialogText.append(spannable)
                        }
                        installFiles.add("https://api.github.com/repos/ThePieMonster/ZygiskFakeEncryption/releases/latest")
                    } else {
                        runOnUiThread {
                            val spannable = SpannableHelper.colorText(Color.GREEN, "\n${texts["dataEncrypted"] ?: "Data is encrypted"}\n")
                            dialogText.append(spannable)
                        }
                    }

                    runOnUiThread {
                        dialogText.append(texts["checkModulesToInstall"])
                    }

                    runOnUiThread { dialogText.append("\n\n${texts["checkModulesToInstall"]}") }
                    val moduleNames = listOf(
                        "BetterKnownInstalled",
                        "playcurlNEXT",
                        "playintegrityfix",
                        "tricky_store",
                        "zn_magisk_compat",
                        "zygisk_lsposed",
                        "zygisk-assistant",
                        "zygisk_shamiko",
                    )

                    val moduleApiUrls = mapOf(
                        "BetterKnownInstalled" to "https://api.github.com/repos/Pixel-Props/BetterKnownInstalled/releases/latest",
                        "playcurlNEXT" to "https://api.github.com/repos/daboynb/playcurlNEXT/releases/latest",
                        "playintegrityfix" to "https://api.github.com/repos/chiteroman/PlayIntegrityFix/releases/latest",
                        "tricky_store" to "https://api.github.com/repos/5ec1cff/TrickyStore/releases/latest",
                        "zn_magisk_compat" to "https://api.github.com/repos/Dr-TSNG/ZygiskNext/releases/latest",
                        "zygisk-assistant" to "https://api.github.com/repos/snake-4/Zygisk-Assistant/releases/latest",
                        "zygisk_shamiko" to "https://api.github.com/repos/LSPosed/LSPosed.github.io/releases/latest",
                    )

                    val APPCHECK = listOf (
                        "com.tsng.hidemyapplist",
                        "io.github.auag0.imnotadeveloper",
                    )

                    val AppsToInstall = mapOf(
                        "com.tsng.hidemyapplist" to "https://github.com/Dr-TSNG/Hide-My-Applist/releases/download/V3.3.1/HMA-V3.3.1.apk",
                        "io.github.auag0.imnotadeveloper" to "https://api.github.com/repos/auag0/ImNotADeveloper/releases/latest"
                    )

                    for (App in APPCHECK) {
                        val check = RootUtils.runCommandWithOutput("pm list packages $App")

                        if (check == "package:$App") {
                            runOnUiThread {
                                val spannable = SpannableHelper.colorText(Color.BLUE, "\n$App ${texts["AppIsInstalled"]}")
                                dialogText.append(spannable)
                            }
                        } else {
                            runOnUiThread {
                                val spannable = SpannableHelper.colorText(Color.RED, "\n$App ${texts["AppNotInstalled"]}")
                                dialogText.append(spannable)
                            }
                            installFiles.add(AppsToInstall[App].toString())
                        }
                    }

                    val lsposed = RootUtils.runCommand("ls /data/adb/modules/zygisk_lsposed") == 0

                    if (lsposed) {
                        runOnUiThread {
                            val spannable = SpannableHelper.colorText(Color.BLUE, "\n${texts["lsposedInstalled"]}")
                            dialogText.append(spannable)
                        }
                    } else {

                        runOnUiThread {
                            val spannable = SpannableHelper.colorText(Color.RED, "\n${texts["lsposedNotInstalled"]}")
                            dialogText.append(spannable) }
                        copyAssetToPath(this@MainActivity, "LSPosed.zip", "/sdcard/download/FixIntegrity/lsposed.zip")
                        val installer = RootUtils.runCommandWithOutput("magisk --install-module /sdcard/download/FixIntegrity/lsposed.zip")
                        runOnUiThread { dialogText.append("\nLSPOSED:\n$installer") }
                    }

                    for (module in moduleNames) {
                        val installed = RootUtils.runCommand("ls /data/adb/modules/$module") == 0
                        runOnUiThread {
                            val statusText = if (installed) {
                                "\n$module ${texts["moduleAlreadyInstalled"]}"
                            } else {
                                "\n$module ${texts["moduleNotInstalled"]}"
                            }
                            val color = if (installed) Color.BLUE else Color.RED
                            val spannable = SpannableHelper.colorText(color, statusText)
                            dialogText.append(spannable)

                            scrollView.post {
                                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                            }
                        }
                        if (!installed) {
                            installFiles.add(moduleApiUrls[module].toString())
                        }
                    }

                    for (assetFileName in installFiles) {
                        runOnUiThread {
                            // Automatisch nach unten scrollen
                            scrollView.post {
                                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                            }
                        }
                        var downloadedFilename: String? = null
                        var downloading = false
                        try {
                            if (assetFileName == "https://github.com/Dr-TSNG/Hide-My-Applist/releases/download/V3.3.1/HMA-V3.3.1.apk") {
                                runOnUiThread {
                                    dialogText.append("\n\n▶ ${texts["download"] ?: "Download"}: HMA-V3.3.1.apk")
                                }

                                val dir = File(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                    "FixIntegrity"
                                )
                                if (!dir.exists()) dir.mkdirs()

                                val targetFile = File(dir, "HMA-V3.3.1.apk")
                                val request = DownloadManager.Request(Uri.parse("https://github.com/Dr-TSNG/Hide-My-Applist/releases/download/V3.3.1/HMA-V3.3.1.apk")).apply {
                                    setTitle("Downloading HMA-V3.3.1.apk")
                                    setDestinationUri(Uri.fromFile(targetFile))
                                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                }

                                val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                val downloadId = downloadManager.enqueue(request)


                                    var downloading = true
                                    while (downloading) {
                                        val query = DownloadManager.Query().setFilterById(downloadId)
                                        val cursor = downloadManager.query(query)
                                        if (cursor != null && cursor.moveToFirst()) {
                                            when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                                                DownloadManager.STATUS_SUCCESSFUL -> {
                                                    downloading = false


                                                }
                                                DownloadManager.STATUS_FAILED -> {
                                                    downloading = false
                                                    runOnUiThread {
                                                        Toast.makeText(this, "❌ ${texts["downloadError"] ?:"Download error"}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        }
                                        cursor?.close()

                                }
                                downloadedFilename = "HMA-V3.3.1.apk"
                            }
                            else if (assetFileName == "LSPosed.zip"){
                                copyAssetToPath(this, "LSPosed.zip", "/sdcard/Download/FixIntegrity/LSPosed.zip")
                                downloadedFilename = "LSPosed.zip"
                            }
                            else{
                                val url = URL(assetFileName)
                                val connection = url.openConnection() as HttpURLConnection
                                connection.connect()

                                val response = connection.inputStream.bufferedReader().use { it.readText() }
                                val json = JSONObject(response)
                                val assets = json.getJSONArray("assets")

                                for (i in 0 until assets.length()) {
                                    val asset = assets.getJSONObject(i)
                                    val name = asset.getString("name")
                                    val downloadUrl = asset.getString("browser_download_url")

                                    if (name.endsWith(".zip") || name.endsWith(".apk")) {
                                        // Zielordner: /sdcard/Download/FixIntegrity/
                                        if (name.endsWith("riru-release.zip")) {
                                            continue
                                        }
                                        runOnUiThread {
                                            dialogText.append("\n\n▶ ${texts["download"] ?: "Download"}: $name")
                                        }
                                        val dir = File(
                                            Environment.getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_DOWNLOADS
                                            ), "FixIntegrity"
                                        )
                                        if (!dir.exists()) dir.mkdirs()
                                        val targetFile = File(dir, name)
                                        val request =
                                            DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                                                setTitle("Downloading: $name")
                                                setDestinationUri(Uri.fromFile(targetFile))
                                            }

                                        val downloadManager =
                                            getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                        val downloadId = downloadManager.enqueue(request)

                                        // Warte bis der Download abgeschlossen ist
                                        var downloading = true
                                        while (downloading) {
                                            val query =
                                                DownloadManager.Query().setFilterById(downloadId)
                                            val cursor = downloadManager.query(query)
                                            if (cursor != null && cursor.moveToFirst()) {
                                                val status = cursor.getInt(
                                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                                                )
                                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                                    downloading = false
                                                } else if (status == DownloadManager.STATUS_FAILED) {
                                                    cursor.close()

                                                }
                                            }
                                            cursor?.close()
                                        }
                                        downloadedFilename = name
                                        // → Hier kannst du weiter mit targetFile arbeiten
                                    }
                                }
                            }

                            val filePath = "/sdcard/Download/FixIntegrity/${downloadedFilename ?: throw IllegalStateException("Dateiname ist null")}"
                            var file = File(filePath)

                            if (downloadedFilename?.endsWith(".zip") == true) {
                                runOnUiThread {
                                    val successMessage = "${texts["installmodul"]} $downloadedFilename"
                                    val spannable = SpannableString(successMessage)

                                    spannable.setSpan(ForegroundColorSpan(Color.GREEN), 0, successMessage.length, 0)

                                    spannable.setSpan(StyleSpan(Typeface.BOLD), 0, successMessage.length, 0)

                                    spannable.setSpan(RelativeSizeSpan(1.1f), 0, successMessage.length, 0)

                                    runOnUiThread {
                                        dialogText.append("\n")
                                        dialogText.append(spannable)
                                    }
                                }

                                val command =
                                    "magisk --install-module /sdcard/Download/FixIntegrity/$downloadedFilename"
                                val process =
                                    Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                                val outputReader = process.inputStream.bufferedReader()
                                val errorReader = process.errorStream.bufferedReader()

                                while (process.isAlive) {
                                    while (outputReader.ready()) {
                                        val line = outputReader.readLine()
                                        runOnUiThread { dialogText.append("\n$line") }
                                    }
                                    while (errorReader.ready()) {
                                        val line = errorReader.readLine()
                                        runOnUiThread { dialogText.append("\n⚠️ $line") }
                                    }

                                }

                                val resultCode = process.waitFor()
                                runOnUiThread {
                                    if (resultCode == 0) {
                                        val successMessage = "✅ ${texts["success"] ?: "success installed"}: $downloadedFilename"
                                        val spannable = SpannableString(successMessage)
                                        spannable.setSpan(ForegroundColorSpan(Color.GREEN), 0, successMessage.length, 0)

                                        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, successMessage.length, 0)

                                        spannable.setSpan(RelativeSizeSpan(1.1f), 0, successMessage.length, 0)

                                        runOnUiThread {
                                            dialogText.append("\n")
                                            dialogText.append(spannable)
                                        }
                                    } else {
                                        val successMessage = "${texts["error"] ?: "Error"}: $downloadedFilename $resultCode)"
                                        val spannable = SpannableString(successMessage)
                                        spannable.setSpan(ForegroundColorSpan(Color.RED), 0, successMessage.length, 0)

                                        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, successMessage.length, 0)

                                        spannable.setSpan(RelativeSizeSpan(1.1f), 0, successMessage.length, 0)

                                        runOnUiThread {
                                            dialogText.append("\n")
                                            dialogText.append(spannable)
                                        }
                                    }

                                }
                            } else if (downloadedFilename?.endsWith(".apk") == true) {
                                runOnUiThread {
                                    val successMessage = "✅ ${texts["installapk"]}: $downloadedFilename"
                                    val spannable = SpannableString(successMessage)
                                    spannable.setSpan(ForegroundColorSpan(Color.GREEN), 0, successMessage.length, 0)

                                    spannable.setSpan(StyleSpan(Typeface.BOLD), 0, successMessage.length, 0)

                                    spannable.setSpan(RelativeSizeSpan(1.1f), 0, successMessage.length, 0)

                                    runOnUiThread {
                                        dialogText.append("\n")
                                        dialogText.append(spannable)
                                    }

                                }
                                val file = File("/sdcard/Download/FixIntegrity/$downloadedFilename")
                                val safeFilename = downloadedFilename.replace("\"", "\\\"")

                                val installCommand = """
                                    mkdir -p /data/local/tmp && \
                                    cp "/sdcard/Download/FixIntegrity/$safeFilename" "/data/local/tmp/$safeFilename" && \
                                    pm install "/data/local/tmp/$safeFilename" && \
                                    rm "/data/local/tmp/$safeFilename"
                                    """.trimIndent()

                                Thread {
                                    try {
                                        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", installCommand))
                                        val resultCode = process.waitFor()

                                        val errorOutput = process.errorStream.bufferedReader().readText()

                                        runOnUiThread {
                                            val spannable: SpannableString
                                            if (resultCode == 0) {
                                                val msg = "✅ ${texts["successinstalled"] ?: "successfully installed"}: $downloadedFilename"
                                                spannable = SpannableString(msg).apply {
                                                    setSpan(ForegroundColorSpan(Color.GREEN), 0, length, 0)
                                                    setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
                                                    setSpan(RelativeSizeSpan(1.1f), 0, length, 0)
                                                }
                                            } else {
                                                val msg = "❌ ${texts["installerror"]} $downloadedFilename (Code: $resultCode)\n$errorOutput"
                                                spannable = SpannableString(msg).apply {
                                                    setSpan(ForegroundColorSpan(Color.RED), 0, length, 0)
                                                    setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
                                                    setSpan(RelativeSizeSpan(1.1f), 0, length, 0)
                                                }
                                            }

                                            dialogText.append("\n")
                                            dialogText.append(spannable)

                                            if (file.exists()) {
                                                file.delete()
                                            }
                                        }

                                    } catch (e: Exception) {
                                        runOnUiThread {
                                            val msg = "❌ ${texts["error"]}: ${e.localizedMessage}"
                                            val spannable = SpannableString(msg).apply {
                                                setSpan(ForegroundColorSpan(Color.RED), 0, length, 0)
                                                setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
                                                setSpan(RelativeSizeSpan(1.1f), 0, length, 0)
                                            }
                                            dialogText.append("\n")
                                            dialogText.append(spannable)
                                        }
                                    }
                                }.start()
                            } else {

                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            runOnUiThread {
                                dialogText.append("\n❌ ${texts["error"]} $assetFileName: ${e.message}")
                            }
                            // Fehler bei einer Datei, aber weiter mit der nächsten Datei fortfahren
                            continue
                        }
                    }

                    runOnUiThread{
                        dialogText.append("\n${texts["copyPifAndKeybox"]}")

                    }

                    copyAssetToPath(this, "keybox.xml", "/data/adb/tricky_store/keybox.xml")
                    copyAssetToPath(this, "pif.json", "/data/adb/pif.json")


                    val shellCommandsForAdd = listOf(
                        "com.android.vending com.android.vending",
                        "com.android.vending com.android.vending:background",
                        "com.android.vending com.android.vending:instant_app_installer",
                        "com.android.vending com.android.vending:quick_launch",
                        "com.android.vending com.android.vending:recovery_mode",
                        "isolated com.android.vending:com.google.android.finsky.verifier.apkanalysis.service.ApkContentsScanService",
                        "com.google.android.gms com.google.android.gms",
                        "com.google.android.gms com.google.android.gms.feedback",
                        "com.google.android.gms com.google.android.gms.learning",
                        "com.google.android.gms com.google.android.gms.persistent",
                        "com.google.android.gms com.google.android.gms.remapping1",
                        "com.google.android.gms com.google.android.gms.room",
                        "com.google.android.gms com.google.android.gms.ui",
                        "com.google.android.gms com.google.android.gms.unstable",
                        "com.google.android.gms com.google.android.gms:car",
                        "com.google.android.gms com.google.android.gms:identitycredentials",
                        "com.google.android.gms com.google.android.gms:snet",
                        "isolated com.google.android.gms:com.google.android.gms.chimera.IsolatedBoundBrokerService"
                    )

                    for (entry in shellCommandsForAdd) {
                        val command = "magisk --denylist add $entry"


                        // Wenn du den Befehl ausführen willst:
                        try {
                            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                            process.waitFor()
                            val result = process.inputStream.bufferedReader().readText()
                            println(result)
                        } catch (e: Exception) {
                            println("Fehler beim Ausführen: ${e.message}")
                        }
                    }
                    val command5 = arrayOf("su", "-c", "rm -rf /sdcard/Download/FixIntegrity")
                    Runtime.getRuntime().exec(command5).waitFor()

                    runOnUiThread {
                        dialogText.append("\n${texts["configureTrickyStore"]}")
                    }

                    val entries = listOf("com.android.vending!", "com.google.android.gms!")
                    val filePath = "/data/adb/tricky_store/target.txt"

                    for (entry in entries) {
                        val checkCommand = arrayOf("su", "-c", "grep -Fxq '$entry' '$filePath'")
                        val checkProcess = Runtime.getRuntime().exec(checkCommand)
                        val exitCode = checkProcess.waitFor()

                        if (exitCode != 0) {
                            // Eintrag nicht gefunden → anhängen
                            val appendCommand = arrayOf("su", "-c", "sh -c 'echo \"$entry\" >> \"$filePath\"'")
                            val appendProcess = Runtime.getRuntime().exec(appendCommand)
                            appendProcess.waitFor()
                            println("Entry \"$entry\" wurde zur Datei hinzugefügt.")
                        } else {
                            println("Entry \"$entry\" ist schon in der Datei.")
                        }
                    }


                    fun getCurrentBootHash(): String {
                        return try {
                            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "getprop ro.boot.vbmeta.digest"))
                            val output = process.inputStream.bufferedReader().readText().trim()
                            process.waitFor()
                            output // kann leer sein, aber nie null
                        } catch (e: Exception) {
                            "" // Fehler → leeren String zurückgeben
                        }
                    }

                    val result = getVerifiedBootHash()
                    val currentHash = getCurrentBootHash()

                    if (currentHash != result) {
                        writeBootHashScript(result)
                    } else {
                    }

                    Runtime.getRuntime().exec(command5).waitFor()

                    copyAssetToPath(this, "get_extra.sh", "/data/adb/get_extra.sh")

                    val scriptPath = "sh /data/adb/get_extra.sh --security-patch"
                    val processBuilder = ProcessBuilder("su", "-c", scriptPath)

                    try {
                        val process = processBuilder.start()
                        val reader = BufferedReader(InputStreamReader(process.inputStream))

                        val outputBuilder = StringBuilder()
                        var outputLine: String?
                        var notSetFound = false

                        while (reader.readLine().also { outputLine = it } != null) {
                            outputBuilder.append(outputLine).append("\n")
                            if (outputLine!!.contains("not set")) {
                                notSetFound = true
                            }
                        }

                        val exitCode = process.waitFor()
                        runOnUiThread {
                            // Show debug output in dialog
                            dialogText.append("\n\nDebug Output:\n$outputBuilder")
                        }

                        if (exitCode == 0 && !notSetFound) {
                            // Create file with su permissions
                            val command = "mkdir -p /data/adb/tricky_store && touch /data/adb/tricky_store/security_patch_auto_config"
                            val suProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                            val suExitCode = suProcess.waitFor()

                            runOnUiThread {
                                if (suExitCode == 0) {
                                    dialogText.append("Automatic configuration successful")
                                } else {
                                    dialogText.append("Error creating file with root permissions")
                                }
                            }
                        } else {
                            runOnUiThread {
                                dialogText.append("Failed: Security patch not set")
                            }
                        }

                        // You can call loadCurrentConfig() here if needed

                    } catch (e: Exception) {
                        runOnUiThread {
                            dialogText.append("Error executing command: ${e.message}")
                        }
                    }

                    runOnUiThread {
                        val successMessage = "\n" +
                                "\n" +
                                "\uD83C\uDF89 ${texts["info"]}"
                        val spannable = SpannableString(successMessage)
                        spannable.setSpan(ForegroundColorSpan(Color.YELLOW), 0, successMessage.length, 0)

                        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, successMessage.length, 0)

                        spannable.setSpan(RelativeSizeSpan(1.1f), 0, successMessage.length, 0)

                    }
                    runOnUiThread {
                        dialogText.append("\n")
                        dialogText.append(texts["finish"])
                        for (button in Buttons) {
                            button.isEnabled = true
                            button.alpha = 1f
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    private fun showBootHashDialog() {

        val selectedLanguage = loadLanguage(this)
        val translations = loadTranslations(this)
        val texts = translations[selectedLanguage] ?: emptyMap()

        val editText = EditText(this).apply {
            hint = texts["deBootHashl2"] ?: "Your Boot Hash"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        AlertDialog.Builder(this)
            .setTitle(texts["deBootHashl2"] ?: "Your Boot Hash")
            .setView(editText)
            .setPositiveButton(texts["SafeButton"] ?: "Safe") { _, _ ->
                val bootHash = editText.text.toString().trim()
                if (bootHash.isNotEmpty()) {
                    writeBootHashScript(bootHash)
                } else {
                    Toast.makeText(this, texts["BootHashEmpty"], Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(texts["Cancle"], null)
            .show()
    }

    private fun writeBootHashScript(bootHash: String) {

        val script = """
        #!/system/bin/sh
        resetprop -n ro.boot.vbmeta.digest $bootHash
        resetprop -n ro.boot.vbmeta.device_state locked
        resetprop -n ro.boot.vbmeta.avb_version 1.2
        resetprop -n ro.boot.vbmeta.hash_alg sha256
        resetprop -n ro.boot.vbmeta.size 4096
    """.trimIndent()

        val filePath = "/data/adb/service.d/vbmeta_fixer.sh"

        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)

            os.writeBytes("echo \"$script\" > $filePath\n")
            os.writeBytes("chmod +x $filePath\n")
            os.writeBytes("exit\n")
            os.flush()
            os.close()

            process.waitFor()

            val process2 = Runtime.getRuntime().exec(arrayOf("su", "-c", "sh /data/adb/service.d/vbmeta_fixer.sh"))
            process2.waitFor()
            runOnUiThread {
                Toast.makeText(this, "Done", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun hideMagiskAndGrantRoot() {
        copyAssetToPath(this, "magsik_manager.apk", "/sdcard/magisk_manager.apk")

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Magisk verstecken...")

        val dialogText = TextView(this).apply {
            setPadding(32, 32, 32, 32)
            text = "Initialisiere..."
            isVerticalScrollBarEnabled = true
        }

        val scrollView = ScrollView(this).apply {
            addView(dialogText)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 200f, resources.displayMetrics
                ).toInt()
            )
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            addView(scrollView)
        }

        dialogBuilder.setView(container)
        dialogBuilder.setPositiveButton("Fertig", null)
        dialogBuilder.setNegativeButton("Neustarten", null)
        dialogBuilder.setCancelable(false)

        val dialog = dialogBuilder.create()
        dialog.show()

        // 🧵 Starte Hintergrund-Thread
        Thread {
            try {
                runOnUiThread {
                    dialogText.append("\n\n🔒 Magisk verstecken...")
                }

                val installProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm install -r /sdcard/magisk_manager.apk"))
                installProcess.waitFor()
                val installError = installProcess.errorStream.bufferedReader().readText()

                runOnUiThread {
                    if (installProcess.exitValue() == 0) {
                        dialogText.append("\n✅ Magisk Manager installiert")
                    } else {
                        dialogText.append("\n❌ Installationsfehler: $installError")
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    dialogText.append("\n\n❌ Ausnahme: ${e.message}")
                }
            }
        }.start()
    }


    fun copyAssetToPath(context: Context, assetFileName: String, targetPath: String) {
        try {
            // Temporäre Datei im internen Cache
            val tempFile = File(context.cacheDir, assetFileName)

            // Datei aus Assets in temporäre Datei kopieren
            context.assets.open(assetFileName).use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Mit Root-Rechten an den Zielort kopieren und ggf. ausführbar machen
            val commands = listOf(
                "cp -f ${tempFile.absolutePath} $targetPath",
            )

            for (cmd in commands) {
                Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor()
            }


        } catch (e: Exception) {
        }
    }


    private fun loadMagiskModulesWithRoot() {
        val selectedLanguage = loadLanguage(this)
        val translations = loadTranslations(this)
        val texts = translations[selectedLanguage] ?: emptyMap()
        val animationView = findViewById<ProgressBar>(R.id.loading_modules_progress)
        animationView.visibility = View.VISIBLE
        val moduleListLayout = findViewById<LinearLayout>(R.id.module_list_container)
        val scrollView = findViewById<ScrollView>(R.id.modules_container)

        moduleListLayout.removeAllViews()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "ls /data/adb/modules/"))
                val modules = process.inputStream.bufferedReader().readLines().filter { it.isNotBlank() }
                process.waitFor()

                withContext(Dispatchers.Main) {
                    if (modules.isEmpty()) {
                        moduleListLayout.addView(TextView(this@MainActivity).apply {
                            text = texts["nomodulefound"] ?: "No Modules Found"
                            setPadding(16, 16, 16, 16)
                            textSize = 16f
                        })
                    } else {
                        allmagiskModules.clear()
                        allmagiskModules.addAll(modules)
                        loadedCount = 0
                        loadNextModules(moduleListLayout)
                        initLazyLoader(scrollView, moduleListLayout)
                    }
                }
            } catch (e: Exception) {

            }

            withContext(Dispatchers.Main) {
                animationView.visibility = View.GONE
            }
        }
    }

    private fun loadNextModules(layout: LinearLayout) {
        val end = (loadedCount + loadBatchSize).coerceAtMost(allmagiskModules.size)
        val sublist = allmagiskModules.subList(loadedCount, end)
        loadedCount = end

        sublist.forEach { module ->
            addModuleCard(module, layout)
        }
    }

    private fun initLazyLoader(scrollView: ScrollView, layout: LinearLayout) {
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val view = scrollView.getChildAt(scrollView.childCount - 1)
            val diff = view.bottom - (scrollView.height + scrollView.scrollY)
            if (diff <= 100) {
                if (loadedCount < allmagiskModules.size) {
                    loadNextModules(layout)
                }
            }
        }
    }

    private fun addModuleCard(moduleName: String, parentLayout: LinearLayout) {
        val propPath = "/data/adb/modules/$moduleName/module.prop"
        val disablePath = "/data/adb/modules/$moduleName/disable"
        val actionPath = "/data/adb/modules/$moduleName/action.sh"

        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val props = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat $propPath"))
                    .inputStream.bufferedReader().readLines()

                val version = props.find { it.startsWith("version=") }?.removePrefix("version=")?.trim() ?: ""
                val versionCode = props.find { it.startsWith("versionCode=") }?.removePrefix("versionCode=")?.trim() ?: ""
                val moduleDisplayName = props.find { it.startsWith("name=") }?.removePrefix("name=").orEmpty().trim()
                val description = props.find { it.startsWith("description=") }?.removePrefix("description=").orEmpty().trim()
                val updateUrl = props.find { it.startsWith("updateJson=") }?.removePrefix("updateJson=").orEmpty().trim()

                val isEnabled = Runtime.getRuntime().exec(arrayOf("su", "-c", "[ ! -f $disablePath ] && echo 1 || echo 0"))
                    .inputStream.bufferedReader().readLine()?.trim() == "1"

                val actionExists = try {
                    val result = Runtime.getRuntime().exec(arrayOf("su", "-c", "[ -f \"$actionPath\" ] && echo exists"))
                        .inputStream.bufferedReader().readText().trim()
                    result == "exists"
                } catch (e: Exception) {
                    false
                }

                withContext(Dispatchers.Main) {
                    val context = this@MainActivity

                    val card = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundResource(R.drawable.card_background)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, 0, 0, 32) }
                        elevation = 8f
                    }

                    val title = TextView(context).apply {
                        val fullText = "$moduleDisplayName\n($version)"
                        val spannable = SpannableString(fullText).apply {
                            val versionStart = fullText.indexOf("($version)")
                            setSpan(RelativeSizeSpan(0.75f), versionStart, fullText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        text = spannable
                        setTextColor(Color.WHITE)
                        textSize = 18f
                        setTypeface(null, Typeface.BOLD)
                    }

                    val desc = TextView(context).apply {
                        text = description
                        setTextColor(Color.WHITE)
                        textSize = 14f
                        setPadding(0, 8, 0, 8)
                    }

                    val switch = Switch(context).apply {
                        isChecked = isEnabled
                        setOnCheckedChangeListener { _, checked ->
                            toggleMagiskModule(moduleName, checked)
                            Toast.makeText(context, if (checked) "✅" else "❌", Toast.LENGTH_SHORT).show()
                        }
                    }

                    val uninstallBtn = ImageView(context).apply {
                        setImageResource(R.drawable.delicon)
                        layoutParams = LinearLayout.LayoutParams(50, 50).apply {
                            gravity = Gravity.START or Gravity.CENTER_VERTICAL
                        }
                        setOnClickListener {
                            CoroutineScope(Dispatchers.IO).launch {
                                Runtime.getRuntime().exec(arrayOf("su", "-c", "rm -rf /data/adb/modules/$moduleName")).waitFor()
                                withContext(Dispatchers.Main) {
                                    parentLayout.removeView(card)
                                }
                            }
                        }
                    }

                    val updateIcon = ImageView(context).apply {
                        setImageResource(R.drawable.update)
                        layoutParams = LinearLayout.LayoutParams(72, 72).apply {
                            gravity = Gravity.END or Gravity.CENTER_VERTICAL
                        }
                        setPadding(0, 0, 16, 0)
                        visibility = View.GONE
                    }

                    val spacer = Space(context).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val actionsLayout = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        addView(uninstallBtn)
                        addView(spacer)
                        if (actionExists) {
                            val actionIcon = ImageView(context).apply {
                                setImageResource(R.drawable.runcommand)
                                layoutParams = LinearLayout.LayoutParams(72, 72).apply {
                                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                                }
                                setPadding(0, 0, 16, 0)
                                setOnClickListener {
                                    runActionFile(actionPath, context)
                                }
                            }
                            addView(actionIcon)
                        }
                        addView(updateIcon)
                    }

                    card.addView(switch)
                    card.addView(title)
                    card.addView(desc)
                    card.addView(actionsLayout)
                    parentLayout.addView(card)

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val connection = URL(updateUrl).openConnection() as HttpURLConnection
                            connection.requestMethod = "GET"
                            connection.connectTimeout = 5000
                            connection.readTimeout = 5000
                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            val json = JSONObject(response)
                            val remoteVersionCode = json.getInt("versionCode")
                            val zipUrl = json.getString("zipUrl")

                            if ((versionCode.toIntOrNull() ?: 0) < remoteVersionCode) {
                                withContext(Dispatchers.Main) {
                                    updateIcon.visibility = View.VISIBLE
                                    updateIcon.setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN)
                                    updateIcon.setOnClickListener {
                                        downloadAndFlashModule(moduleName, zipUrl, updateIcon)
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun downloadAndFlashModule(moduleName: String, zipUrl: String, updateIcon: ImageView) {
        val selectedLanguage = loadLanguage(this)
        val translations = loadTranslations(this)
        val texts = translations[selectedLanguage] ?: emptyMap()
        val builder = AlertDialog.Builder(this)
        val propPath = "/data/adb/modules/$moduleName/module.prop"
        builder.setTitle(texts["Install"] ?: "Install")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }

        val progressBar = ProgressBar(this).apply {
            isIndeterminate = true
        }

        layout.addView(progressBar)
        layout.addView(statusText)
        builder.setView(layout)

        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    statusText.text = "🌐 ${texts["download"] ?: "Download"}"
                }

                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val fixIntegrityDir = File(downloadDir, "FixIntegrity")
                if (!fixIntegrityDir.exists()) fixIntegrityDir.mkdirs()

                val destFile = File(fixIntegrityDir, "$moduleName.zip")

                val url = URL(zipUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                BufferedInputStream(connection.inputStream).use { input ->
                    FileOutputStream(destFile).use { output ->
                        val data = ByteArray(4096)
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            output.write(data, 0, count)
                        }
                        output.flush()
                    }
                }

                withContext(Dispatchers.Main) {
                    statusText.text = "⚙️ ${texts["Install"] ?: "Install"}"
                }

                val cmd = "magisk --install-module \"${destFile.absolutePath}\""
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                val result = process.waitFor()

                withContext(Dispatchers.Main) {
                    val props = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat $propPath"))
                        .inputStream.bufferedReader().readLines()

                    loadMagiskModulesWithRoot()
                    statusText.text = if (result == 0) {
                        "✅"
                    } else {
                        "❌"
                    }
                    delay(3000)
                    dialog.dismiss()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    statusText.text = "❌ Error: ${e.message}"
                    delay(3000)
                    dialog.dismiss()
                }
            }
        }
    }

    data class ModuleUpdateInfo(
        val name: String,
        val latestVersion: String,
        val downloadUrl: String
    )

    fun runActionFile(actionPath: String, context: Context) {
        val dialogText = TextView(context).apply {
            setPadding(32, 32, 32, 32)
            text = "\n\n"
            isVerticalScrollBarEnabled = true
            movementMethod = ScrollingMovementMethod()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val scrollView = ScrollView(context).apply {
            addView(dialogText)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    300f,
                    context.resources.displayMetrics
                ).toInt()
            )
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            addView(scrollView)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("")
            .setView(container)
            .setCancelable(false)
            .setPositiveButton("OK", null)
            .create()

        dialog.show()

        Thread {
            try {
                // Datei prüfen
                val checkProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", "[ -f \"$actionPath\" ] && echo exists"))
                val checkResult = checkProcess.inputStream.bufferedReader().readText().trim()
                checkProcess.waitFor()

                if (checkResult != "exists") {
                    (context as Activity).runOnUiThread {
                        dialogText.append("❌: $actionPath")
                    }
                    return@Thread
                }

                // Ausführen
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "sh \"$actionPath\""))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    (context as Activity).runOnUiThread {
                        dialogText.append("📤 $line\n")
                    }
                }

                var errorLine: String?
                while (errorReader.readLine().also { errorLine = it } != null) {
                    (context as Activity).runOnUiThread {
                        dialogText.append("⚠️ $errorLine\n")
                    }
                }

                process.waitFor()
                (context as Activity).runOnUiThread {
                    dialogText.append("\n${process.exitValue()})")
                }
            } catch (e: Exception) {
                (context as Activity).runOnUiThread {
                    dialogText.append("\n❌ Error: ${e.message}")
                }
            }
        }.start()
    }



    @OptIn(UnstableApi::class)
    fun checkAndUpdateModuleWithRoot(module: ModuleUpdateInfo) {
        Thread {
            try {
                val propFile = "/data/adb/modules/${module.name}/module.prop"
                val getVersionCommand = "cat $propFile | grep version="
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", getVersionCommand))
                val installedVersion = process.inputStream.bufferedReader().readText()
                    .removePrefix("version=").trim()

                if (installedVersion != module.latestVersion) {

                    // Datei-Pfad wohin ZIP geladen wird
                    val zipPath = "/sdcard/Download/${module.name}.zip"

                    // Mit curl als root downloaden
                    val downloadCmd = "curl -L '${module.downloadUrl}' -o '$zipPath'"
                    Runtime.getRuntime().exec(arrayOf("su", "-c", downloadCmd)).waitFor()

                    // ZIP mit Magisk installieren
                    val installCmd = "magisk --install-module '$zipPath'"
                    val installProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", installCmd))
                    val resultCode = installProcess.waitFor()

                    if (resultCode == 0) {
                    } else {
                    }
                } else {
                }

            } catch (e: Exception) {
            }
        }.start()
    }


    private fun toggleMagiskModule(moduleName: String, isChecked: Boolean) {
        Thread {
            try {
                val command = if (isChecked) {
                    // Aktivieren → disable-Datei löschen
                    "rm /data/adb/modules/$moduleName/disable"
                } else {
                    // Deaktivieren → disable-Datei erstellen
                    "touch /data/adb/modules/$moduleName/disable"
                }

                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                process.waitFor()

                val resultCode = process.exitValue()
                runOnUiThread {
                    if (resultCode == 0) {
                        Toast.makeText(
                            this,
                            if (isChecked) "✅: $moduleName" else "🚫: $moduleName",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "❌ Error: $moduleName",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {

            }
        }.start()
    }


    data class ModuleInfo(
        val name: String,
        val version: String,
        val description: String
    )

    private fun clearImportantApps() {
        val results = importantSystemApps.map { packageName ->
            val result = clearAppData1(packageName)
            "[${if (result) "✔" else "❌"}] $packageName ${if (result) "Deleted" else "not deleted"}"
        }.joinToString("\n")

        // Neustart des Geräts mit root-Rechten
        Thread {
            try {
                val command = "reboot"
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                process.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    statusText.text = "❌ Error: ${e.message}"
                }
            }
        }.start()

        runOnUiThread {
            statusText.text = results
        }
    }


    private fun clearAppData1(packageName: String): Boolean {
        return try {
            val command = "pm clear $packageName"
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val resultCode = process.waitFor()
            Toast.makeText(this@MainActivity, "Delete: $packageName ...", Toast.LENGTH_SHORT).show()

            resultCode == 0

        } catch (e: Exception) {
            e.printStackTrace()

            false
        }
    }

    private fun showSection(container: View, isBack: Boolean = false) {
        val allContainers = listOf(appListContainer, lsContainer, settingsContainer, BackupContainer, moduleContainer, homeContainer, languagescroll, tricky_store_container, module_containerr)

        val duration = 300L
        val currentContainer = allContainers.find { it.visibility == View.VISIBLE }

        // Wenn der aktuelle Container der gleiche wie der neue ist, keinen Wechsel vornehmen
        if (currentContainer == container) return

        // Zoom-Out für den alten Container
        currentContainer?.let {
            it.animate()
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(duration)
                .withEndAction {
                    it.visibility = View.GONE
                    it.scaleX = 1f
                    it.scaleY = 1f
                    it.alpha = 1f // Rücksetzen der Alpha und Scale-Werte
                }
                .start()
        }

        // Vorbereiten: Neuer Container
        container.scaleX = 0f
        container.scaleY = 0f
        container.alpha = 0f
        container.visibility = View.VISIBLE

        val linearLayout = findViewById<LinearLayout>(R.id.buttononhome)

        // Zoom-In für den neuen Container
        container.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(duration)
            .start()

        if (container == homeContainer) {
            linearLayout.visibility = View.VISIBLE
        } else {
            linearLayout.visibility = View.GONE
        }
        // Backstack nur aktualisieren, wenn es vorwärts ist
        if (!isBack && containerBackStack.lastOrNull() != container) {
            containerBackStack.add(container)
        }
    }

    override fun onBackPressed() {
        if (containerBackStack.size > 1) {
            // Aktuellen Container entfernen
            containerBackStack.removeAt(containerBackStack.lastIndex)


            // Vorherigen Container anzeigen
            val previousContainer = containerBackStack.last()
            showSection(previousContainer, isBack = true)
        } else {
            super.onBackPressed() // App schließen oder Standardverhalten
        }
    }

    private fun loadInstalledAppsBackup(list: LinearLayout, query: String = "") {
        val lodingspinner = findViewById<ProgressBar>(R.id.loading_spinner_backup)
        lodingspinner.visibility = View.VISIBLE
        list.removeAllViews()

        lifecycleScope.launch {
            allApps = withContext(Dispatchers.IO) {
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { app ->
                        val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val isUpdatedSystemApp = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                        val isUserApp = !isSystemApp || isUpdatedSystemApp
                        val appName = app.loadLabel(packageManager).toString()
                        val packageName = app.packageName
                        val matchesQuery = query.isBlank() ||
                                appName.contains(query, ignoreCase = true) ||
                                packageName.contains(query, ignoreCase = true)
                        ((showUserAppsBackup && isUserApp) || (showSystemAppsBackup && !isUserApp)) && matchesQuery
                    }.sortedBy { it.loadLabel(packageManager).toString().lowercase() }
            }

            loadAllAppsBackup(list)

            withContext(Dispatchers.Main) {
                lodingspinner.visibility = View.GONE
            }
        }
    }


    private fun loadAllAppsBackup(list: LinearLayout) {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                for (app in allApps) {
                    val card = createAppCardViewBackup(app)
                    list.addView(card)
                }
            }
        }
    }

    private fun createAppCardViewBackup(app: ApplicationInfo): View {
        val context = this
        val cardView = CardView(this@MainActivity).apply {
            radius = 16f
            cardElevation = 8f
            setPadding(0, 10, 0, 10)
            useCompatPadding = true
            setContentPadding(16, 18, 16, 18)
            setBackgroundResource(R.drawable.rounded_container2)

            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }

        // Horizontal Layout für Icon und CheckBox
        val rowLayout = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, // Maximale Breite für RowLayout
                LinearLayout.LayoutParams.WRAP_CONTENT // Der Inhalt bestimmt die Höhe
            )
        }

        val iconView = ImageView(this@MainActivity).apply {
            setImageDrawable(app.loadIcon(packageManager))
            layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                marginEnd = 16
            }
        }

        // Layout für den App-Namen und Paketnamen (vertikal)
        val nameAndPackageLayout = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                weight = 1f // Macht dieses Layout flexibel und ermöglicht es, den verfügbaren Platz gut zu nutzen
            }
        }

        val nameView = TextView(this@MainActivity).apply {
            text = app.loadLabel(packageManager).toString()
            textSize = 16f // Schriftgröße für den App-Namen
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setTextColor(Color.WHITE)
        }

        val packednameView = TextView(this@MainActivity).apply {
            text = app.packageName
            textSize = 12f // Kleinere Schriftgröße für den Paketnamen
            setTextColor(Color.parseColor("#808080")) // Graue Farbe
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 4 // Abstand zwischen App-Namen und Paketnamen
            }
        }

        // Prozesserstellung
        val processes = mutableSetOf<String>()
        val processViews = mutableListOf<TextView>() // Liste für alle TextViews der Prozesse

        try {
            val pkgInfo = packageManager.getPackageInfo(app.packageName,
                PackageManager.GET_SERVICES or
                        PackageManager.GET_RECEIVERS or
                        PackageManager.GET_PROVIDERS or
                        PackageManager.GET_ACTIVITIES
            )

            pkgInfo.services?.forEach { it.processName?.let { p -> processes.add(p) } }
            pkgInfo.receivers?.forEach { it.processName?.let { p -> processes.add(p) } }
            pkgInfo.providers?.forEach { it.processName?.let { p -> processes.add(p) } }
            pkgInfo.activities?.forEach { it.processName?.let { p -> processes.add(p) } }
        } catch (e: Exception) {
            e.printStackTrace() // Fehlerbehandlung
        }

        // Layout für alle Prozess-TextViews (initial versteckt)
        val processLayout = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE // Anfangs unsichtbar
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        // Füge für jeden Prozess ein eigenes TextView hinzu
        processes.forEach { process ->
            val processTextView = TextView(this@MainActivity).apply {
                textSize = 12f
                setTextColor(Color.parseColor("#CCCCCC"))
                text = process
                layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 4
                }
            }
            processViews.add(processTextView)
            processLayout.addView(processTextView)
        }

        // Klickbarer Bereich (z. B. der App-Name)
        nameView.setOnClickListener {
            // Sichtbarkeit umschalten
            processLayout.visibility = if (processLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val checkBox = CheckBox(this@MainActivity).apply {
            isChecked = selectedPackagesBackup.contains(app.packageName)
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedPackagesBackup.add(app.packageName)
                else selectedPackagesBackup.remove(app.packageName)
            }
        }

        // Füge die Views für Name und Paketname in das vertikale Layout hinzu
        nameAndPackageLayout.addView(nameView)
        nameAndPackageLayout.addView(packednameView)
        nameAndPackageLayout.addView(processLayout)
        // Füge das vertikale Layout (mit App-Name und Paketname) sowie die CheckBox zum horizontalen Layout hinzu
        rowLayout.addView(iconView)
        rowLayout.addView(nameAndPackageLayout) // Hier das neue Layout mit Name und Paketname einfügen
        rowLayout.addView(checkBox)

        // Füge das RowLayout in die CardView ein
        cardView.addView(rowLayout)

        return cardView
    }

    private fun loadInstalledApps(list: LinearLayout, query: String = "") {
        val lodingspinner = findViewById<ProgressBar>(R.id.loading_spinner)
        lodingspinner.visibility = View.VISIBLE
        list.removeAllViews()



        lifecycleScope.launch {
            allApps = withContext(Dispatchers.IO) {
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { app ->
                        val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val isUpdatedSystemApp = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                        val isUserApp = !isSystemApp || isUpdatedSystemApp
                        val appName = app.loadLabel(packageManager).toString()
                        val packageName = app.packageName
                        val matchesQuery = query.isBlank() ||
                                appName.contains(query, ignoreCase = true) ||
                                packageName.contains(query, ignoreCase = true)
                        ((showUserAppsDenylist && isUserApp) || (showSystemAppsDenylist && !isUserApp)) && matchesQuery
                    }.sortedBy { it.loadLabel(packageManager).toString().lowercase() }
            }

            loadAllApps(list)

            withContext(Dispatchers.Main) {
                lodingspinner.visibility = View.GONE
            }
        }
    }


    private fun loadAllApps(list: LinearLayout) {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                for (app in allApps) {
                    val card = createAppCardView(app)
                    list.addView(card)
                }
            }
        }
    }

    private fun createAppCardView(app: ApplicationInfo): View {
        val context = this
        val cardView = CardView(this@MainActivity).apply {
            radius = 16f
            cardElevation = 8f
            setPadding(0, 10, 0, 10)
            useCompatPadding = true
            setContentPadding(16, 18, 16, 18)
            setBackgroundResource(R.drawable.rounded_container2)

            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }

        // Horizontal Layout für Icon und CheckBox
        val rowLayout = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, // Maximale Breite für RowLayout
                LinearLayout.LayoutParams.WRAP_CONTENT // Der Inhalt bestimmt die Höhe
            )
        }

        val iconView = ImageView(this@MainActivity).apply {
            setImageDrawable(app.loadIcon(packageManager))
            layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                marginEnd = 16
            }
        }

        // Layout für den App-Namen und Paketnamen (vertikal)
        val nameAndPackageLayout = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                weight = 1f // Macht dieses Layout flexibel und ermöglicht es, den verfügbaren Platz gut zu nutzen
            }
        }

        val nameView = TextView(this@MainActivity).apply {
            text = app.loadLabel(packageManager).toString()
            textSize = 16f // Schriftgröße für den App-Namen
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setTextColor(Color.WHITE)
        }

        val packednameView = TextView(this@MainActivity).apply {
            text = app.packageName
            textSize = 12f // Kleinere Schriftgröße für den Paketnamen
            setTextColor(Color.parseColor("#808080")) // Graue Farbe
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 4 // Abstand zwischen App-Namen und Paketnamen
            }
        }

        // Prozesserstellung
        val processes = mutableSetOf<String>()
        val processViews = mutableListOf<TextView>() // Liste für alle TextViews der Prozesse

        try {
            val pkgInfo = packageManager.getPackageInfo(app.packageName,
                PackageManager.GET_SERVICES or
                        PackageManager.GET_RECEIVERS or
                        PackageManager.GET_PROVIDERS or
                        PackageManager.GET_ACTIVITIES
            )

            pkgInfo.services?.forEach { it.processName?.let { p -> processes.add(p) } }
            pkgInfo.receivers?.forEach { it.processName?.let { p -> processes.add(p) } }
            pkgInfo.providers?.forEach { it.processName?.let { p -> processes.add(p) } }
            pkgInfo.activities?.forEach { it.processName?.let { p -> processes.add(p) } }
        } catch (e: Exception) {
            e.printStackTrace() // Fehlerbehandlung
        }

        // Layout für alle Prozess-TextViews (initial versteckt)
        val processLayout = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE // Anfangs unsichtbar
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        // Füge für jeden Prozess ein eigenes TextView hinzu
        processes.forEach { process ->
            val processTextView = TextView(this@MainActivity).apply {
                textSize = 12f
                setTextColor(Color.parseColor("#CCCCCC"))
                text = process
                layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 4
                }
            }
            processViews.add(processTextView)
            processLayout.addView(processTextView)
        }

        // Klickbarer Bereich (z. B. der App-Name)
        nameView.setOnClickListener {
            // Sichtbarkeit umschalten
            processLayout.visibility = if (processLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val checkBox = CheckBox(this@MainActivity).apply {
            isChecked = selectedPackages.contains(app.packageName)
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedPackages.add(app.packageName)
                else selectedPackages.remove(app.packageName)
            }
        }

        // Füge die Views für Name und Paketname in das vertikale Layout hinzu
        nameAndPackageLayout.addView(nameView)
        nameAndPackageLayout.addView(packednameView)
        nameAndPackageLayout.addView(processLayout)
        // Füge das vertikale Layout (mit App-Name und Paketname) sowie die CheckBox zum horizontalen Layout hinzu
        rowLayout.addView(iconView)
        rowLayout.addView(nameAndPackageLayout) // Hier das neue Layout mit Name und Paketname einfügen
        rowLayout.addView(checkBox)

        // Füge das RowLayout in die CardView ein
        cardView.addView(rowLayout)

        return cardView
    }

    private fun loadInstalledAppsDenylist(list: LinearLayout, query: String = "") {
        val lodingspinner = findViewById<ProgressBar>(R.id.loading_spinner_denylist)
        lodingspinner.visibility = View.VISIBLE
        list.removeAllViews()

        lifecycleScope.launch {
            // Führe die Initialisierung im Hintergrund durch
            allApps = withContext(Dispatchers.IO) {
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { app ->
                        val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val isUpdatedSystemApp = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                        val isUserApp = !isSystemApp || isUpdatedSystemApp
                        val appName = app.loadLabel(packageManager).toString()
                        val packageName = app.packageName
                        val matchesQuery = query.isBlank() ||
                                appName.contains(query, ignoreCase = true) ||
                                packageName.contains(query, ignoreCase = true)
                        ((showUserAppsDenylist && isUserApp) || (showSystemAppsDenylist && !isUserApp)) && matchesQuery
                    }.sortedBy { it.loadLabel(packageManager).toString().lowercase() }
            }

            // Alle Apps auf einmal laden und in das Layout einfügen
            loadAllAppsDenylist(list)

            // Warten Sie, bis die Apps geladen sind, bevor die UI-Komponenten aktualisiert werden
            withContext(Dispatchers.Main) {
                lodingspinner.visibility = View.GONE
            }
        }
    }

    private fun loadAllAppsDenylist(list: LinearLayout) {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                // Die Denylist holen
                val denylist = getMagiskDenylist()

                val sortedApps = allApps.sortedWith(compareByDescending { denylist.contains(it.packageName) })

                for (app in sortedApps) {
                    val card = createAppCardViewDenylist(app, denylist)
                    list.addView(card)
                }
            }
        }
    }

    private fun createAppCardViewDenylist(app: ApplicationInfo, denylist: Set<String>): View {
        val context = this

        val cardView = CardView(this@MainActivity).apply {
            radius = 16f
            cardElevation = 8f
            setPadding(0, 10, 0, 10)
            useCompatPadding = true
            setContentPadding(16, 18, 16, 18)
            setBackgroundResource(R.drawable.rounded_container2)
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 8, 0, 8)
            }
        }

        val rowLayout = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 10
            }
        }

        val iconView = ImageView(this@MainActivity).apply {
            setImageDrawable(app.loadIcon(packageManager))
            layoutParams = LinearLayout.LayoutParams(100, 100).apply { marginEnd = 16 }
        }

        val nameAndPackageLayout = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val nameView = TextView(this@MainActivity).apply {
            text = app.loadLabel(packageManager).toString()
            textSize = 16f
            setTextColor(Color.WHITE)
        }

        val packednameView = TextView(this@MainActivity).apply {
            text = app.packageName
            textSize = 12f
            setTextColor(Color.parseColor("#808080"))
            layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 4
            }
        }

        // App-Haupt-Checkbox
        val appCheckBox = CheckBox(this@MainActivity).apply {
            isChecked = denylist.contains(app.packageName) // Magisk Denylist prüfen
            setOnCheckedChangeListener { _, isChecked ->
                val packageName = app.packageName
                val command = if (!isChecked) {
                    // Von Denylist entfernen
                    "magisk --denylist rm $packageName"
                } else {
                    // Zur Denylist hinzufügen
                    "magisk --denylist add $packageName"
                }

                try {
                    Runtime.getRuntime().exec(arrayOf("su", "-c", command)).waitFor()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val processes = mutableSetOf<String>()
        val processViews = mutableListOf<CheckBox>() // Liste für alle CheckBoxen der Prozesse

        try {
            val pkgInfo = packageManager.getPackageInfo(app.packageName,
                PackageManager.GET_SERVICES or
                        PackageManager.GET_RECEIVERS or
                        PackageManager.GET_PROVIDERS or
                        PackageManager.GET_ACTIVITIES
            )

            pkgInfo.services?.forEach { it.processName?.let { p -> processes.add(p) } }
            pkgInfo.receivers?.forEach { it.processName?.let { p -> processes.add(p) } }
            pkgInfo.providers?.forEach { it.processName?.let { p -> processes.add(p) } }
            pkgInfo.activities?.forEach { it.processName?.let { p -> processes.add(p) } }
        } catch (e: Exception) {
            e.printStackTrace() // Fehlerbehandlung
        }

        // Layout für alle Prozess-CheckBoxen (initial versteckt)
        val processLayout = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE // Anfangs unsichtbar

            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 150
            }
        }

        // Füge für jeden Prozess eine eigene CheckBox hinzu
        processes.forEach { process ->
            val processCheckBox = CheckBox(this@MainActivity).apply {
                text = process
                setTextColor(Color.parseColor("#64B5F6"))
                isChecked = denylist.contains(process) // Wenn der Prozess in der Denylist ist, Checkbox aktivieren
                setOnCheckedChangeListener { _, isChecked ->
                    val packageName = app.packageName
                    val command = if (!isChecked) {
                        // Von Denylist entfernen
                        "magisk --denylist rm $packageName $process"
                    } else {
                        // Zur Denylist hinzufügen
                        "magisk --denylist add $packageName $process"
                    }
                    try {
                        Runtime.getRuntime().exec(arrayOf("su", "-c", command)).waitFor()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            processViews.add(processCheckBox)
            processLayout.addView(processCheckBox)
        }

        nameView.setOnClickListener {
            processLayout.visibility = if (processLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        nameAndPackageLayout.addView(nameView)
        nameAndPackageLayout.addView(packednameView)

        rowLayout.addView(iconView)
        rowLayout.addView(nameAndPackageLayout)
        rowLayout.addView(appCheckBox) // Checkbox für die App hinzufügen

        // Füge zuerst das rowLayout hinzu und dann das processLayout
        cardView.addView(rowLayout)
        cardView.addView(processLayout) // Prozess-Layout hinzugefügt nach dem rowLayout

        return cardView
    }


    private fun loadInstalledAppsTrickyStore(list: LinearLayout, query: String = "") {
        val lodingspinner = findViewById<ProgressBar>(R.id.loading_spinner_tricky)
        lodingspinner.visibility = View.VISIBLE
        list.removeAllViews()

        lifecycleScope.launch {
            // Führe die Initialisierung im Hintergrund durch
            allApps = withContext(Dispatchers.IO) {
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { app ->
                        val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val isUpdatedSystemApp = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                        val isUserApp = !isSystemApp || isUpdatedSystemApp
                        val appName = app.loadLabel(packageManager).toString()
                        val packageName = app.packageName
                        val matchesQuery = query.isBlank() ||
                                appName.contains(query, ignoreCase = true) ||
                                packageName.contains(query, ignoreCase = true)
                        ((showUserAppsTrickyStore && isUserApp) || (showSystemAppsTrickyStore && !isUserApp)) && matchesQuery
                    }.sortedBy { it.loadLabel(packageManager).toString().lowercase() }
            }

            // Alle Apps auf einmal laden und in das Layout einfügen
            loadAllAppsTrickyStore(list)

            // Warten Sie, bis die Apps geladen sind, bevor die UI-Komponenten aktualisiert werden
            withContext(Dispatchers.Main) {

                lodingspinner.visibility = View.GONE
            }
        }
    }

    private fun loadAllAppsTrickyStore(list: LinearLayout) {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                // Die Denylist holen
                val targets = readTrickyStoreTargets()

                val sortedApps = allApps.sortedWith(compareByDescending { targets.contains(it.packageName) })

                for (app in sortedApps) {
                    val card = createAppCardViewTrickyStore(app, targets)
                    list.addView(card)
                }
            }
        }
    }

    private fun createAppCardViewTrickyStore(app: ApplicationInfo, targets: Set<String>): View {
        val context = this

        val cardView = CardView(this@MainActivity).apply {
            radius = 16f
            cardElevation = 8f
            setPadding(0, 10, 0, 10)
            useCompatPadding = true
            setContentPadding(16, 18, 16, 18)
            setBackgroundResource(R.drawable.rounded_container2)
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 8, 0, 8)
            }
        }

        val rowLayout = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 10
            }
        }

        val iconView = ImageView(this@MainActivity).apply {
            setImageDrawable(app.loadIcon(packageManager))
            layoutParams = LinearLayout.LayoutParams(100, 100).apply { marginEnd = 16 }
        }

        val nameAndPackageLayout = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val nameView = TextView(this@MainActivity).apply {
            text = app.loadLabel(packageManager).toString()
            textSize = 16f
            setTextColor(Color.WHITE)
        }

        val packednameView = TextView(this@MainActivity).apply {
            text = app.packageName
            textSize = 12f
            setTextColor(Color.parseColor("#808080"))
            layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 4
            }
        }

        val appCheckBox = CheckBox(this@MainActivity).apply {
            isChecked = targets.contains(app.packageName)
            setOnCheckedChangeListener { _, isChecked ->
                updateTrickyTargetList(app.packageName, isChecked)
            }
        }

        nameAndPackageLayout.addView(nameView)
        nameAndPackageLayout.addView(packednameView)

        rowLayout.addView(iconView)
        rowLayout.addView(nameAndPackageLayout)
        rowLayout.addView(appCheckBox)

        cardView.addView(rowLayout)

        return cardView
    }

    private fun readTrickyStoreTargets(): MutableSet<String> {
        return try {
            val process = ProcessBuilder("su", "-c", "cat /data/adb/tricky_store/target.txt")
                .redirectErrorStream(true)
                .start()
            val result = process.inputStream.bufferedReader().readLines().toMutableSet()
            result
        } catch (e: Exception) {
            mutableSetOf()
        }
    }

    private fun updateTrickyTargetList(target: String, add: Boolean) {
        val cmd = if (add) {
            // Nur hinzufügen, wenn es nicht bereits drin ist
            "[ ! -f /data/adb/tricky_store/target.txt ] && touch /data/adb/tricky_store/target.txt; " +
                    "grep -qxF \"$target\" /data/adb/tricky_store/target.txt || echo \"$target\" >> /data/adb/tricky_store/target.txt"
        } else {
            "sed -i '/^$target\$/d' /data/adb/tricky_store/target.txt"
        }

        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun getMagiskDenylist(): Set<String> {
        val result = mutableSetOf<String>()

        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "magisk --denylist ls"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    val parts = it.split("|") // Aufteilen der Zeilen nach dem '|' Zeichen
                    if (parts.size > 1) {
                        // Extrahiere den Prozessnamen (der nach dem '|')
                        result.add(parts[1].trim())
                    }
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    private fun clearAppData(packageName: String): Boolean {
        return try {
            val command = "pm clear $packageName"
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor()
            val output = process.inputStream.bufferedReader().readText()
            output.contains("Success")
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun installModulesFromUris(uris: List<Uri>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("installing")

        val listformodules = mutableListOf<Any>()
        val context = this
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }

        val progressBar = ProgressBar(this).apply {
            isIndeterminate = true
        }

        val statusText = TextView(this).apply {
            text = "⏳"
            textSize = 16f
            setPadding(0, 0, 0, 16)
        }

        val logTextView = TextView(this).apply {
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }

        val scrollView = HorizontalScrollView(this).apply {
            val innerScroll = ScrollView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT

                )
                addView(logTextView)
            }
            addView(innerScroll)
        }

        layout.addView(progressBar)
        layout.addView(scrollView)
        layout.addView(statusText)
        builder.setView(layout)

        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "FixIntegrity")
                if (!folder.exists()) folder.mkdirs()

                for ((index, uri) in uris.withIndex()) {
                    withContext(Dispatchers.Main) {
                        statusText.text = "📦 Download: ${index + 1}/${uris.size}"
                    }

                    val fileName = "modul_$index.zip"
                    val destFile = File(folder, fileName)
                    listformodules.add(destFile)
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        statusText.text = "⚙️ ${index + 1} Flash..."
                    }

                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "magisk --install-module ${destFile.absolutePath}"))

                    // Lies stdout/stderr
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        withContext(Dispatchers.Main) {
                            logTextView.append("🔹 $line\n")
                            scrollView.fullScroll(View.FOCUS_DOWN)
                        }
                    }
                    while (errorReader.readLine().also { line = it } != null) {
                        withContext(Dispatchers.Main) {
                            logTextView.append("❗ $line\n")
                            scrollView.fullScroll(View.FOCUS_DOWN)
                        }
                    }

                    val result = process.waitFor()

                    withContext(Dispatchers.Main) {
                        statusText.text = if (result == 0)
                            "✅"
                        else
                            "❌"

                        delay(1500)
                    }
                }

                withContext(Dispatchers.Main) {
                    statusText.text = "🎉 Done!"
                    delay(3000)
                    dialog.dismiss()
                }
                for (f in listformodules) {
                    Runtime.getRuntime().exec(arrayOf("su", "-c", "rm -rf $f"))
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "❌ Error: ${e.message}"
                    logTextView.append("❌ Exception: ${e.message}\n")
                    delay(3000)
                }
            }
        }
    }

    fun fetchModulesFromWeb(query: String = "") {
        loading = true
        val loadingSpinner = findViewById<ProgressBar>(R.id.loading_ls_modules)
        runOnUiThread { loadingSpinner.visibility = View.VISIBLE }

        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://modules.lsposed.org/modules.json")
                    .build()
                val response = client.newCall(request).execute()
                val json = response.body?.string()
                val jsonArray = JSONArray(json)

                allModules.clear()

                for (i in 0 until jsonArray.length()) {
                    var apiurl = ""
                    val obj = jsonArray.getJSONObject(i)
                    val title = obj.optString("name", "")

                    val releases = obj.optJSONArray("releases")
                    var downloadurl = ""

                    if (releases != null && releases.length() > 0) {
                        val firstRelease = releases.getJSONObject(0)
                        val releaseAssets = firstRelease.optJSONArray("releaseAssets")

                        if (releaseAssets != null && releaseAssets.length() > 0) {
                            val firstAsset = releaseAssets.getJSONObject(0)
                            downloadurl = firstAsset.optString("downloadUrl", "")
                        }
                    }

                    val autor = obj.optString("name", "")
                    val description = obj.optString("description", "")
                    val homepageUrl = when {
                        obj.has("homepageUrl") && obj.getString("homepageUrl").isNotBlank() -> obj.getString("homepageUrl")
                        obj.has("homepage Url") && obj.getString("homepage Url").isNotBlank() -> obj.getString("homepage Url")
                        obj.has("url") && obj.getString("url").isNotBlank() -> obj.getString("url")
                        else -> ""
                    }

                    val regex = Regex("https://github.com/([^/]+/[^/]+)")
                    val match = regex.find(homepageUrl)
                    if (match != null) {
                        val repoPath = match.groupValues[1]
                        apiurl = "https://api.github.com/repos/$repoPath/releases/latest"
                    }

                    allModules.add(lsModuleInfo(title, description, homepageUrl, downloadurl, apiurl))
                }

                // Filter anwenden
                filteredModules.clear()
                filteredModules.addAll(
                    if (query.isEmpty()) allModules
                    else allModules.filter { it.title.contains(query, ignoreCase = true) }
                )

                runOnUiThread {
                    page = 0
                    container.removeAllViews()
                    loadNextPage()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                runOnUiThread {
                    loadingSpinner.visibility = View.GONE
                }
            }
        }.start()
    }
    private fun loadNextPage() {
        loading = true
        val start = page * pageSize
        val end = minOf(start + pageSize, filteredModules.size)

        for (i in start until end) {
            val module = filteredModules[i]

            val moduleLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.rounded_container)
                setPadding(24, 24, 24, 24)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 24)
                layoutParams = params
            }

            moduleLayout.setOnClickListener {
                showModuleDialog(module)
            }

            val titleView = TextView(this).apply {
                text = "📦 ${module.description}"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            }

            val descView = TextView(this).apply {
                text = module.title
                textSize = 14f
                setTextColor(Color.LTGRAY)
                setPadding(0, 8, 0, 0)
            }

            val urlView = TextView(this).apply {
                text = module.homepageUrl
                textSize = 13f
                setTextColor(Color.CYAN)
                setPadding(0, 8, 0, 0)
            }

            val authorView = TextView(this).apply {
                text = "👤 "
                textSize = 13f
                setTextColor(Color.GRAY)
                setPadding(0, 8, 0, 0)
            }

            moduleLayout.addView(titleView)
            moduleLayout.addView(descView)
            moduleLayout.addView(urlView)
            moduleLayout.addView(authorView)
            container.addView(moduleLayout)
        }

        page++
        loading = false
    }
    private fun showModuleDialog(module: lsModuleInfo) {
        val message = """
        📦 Titel: ${module.description}
        
        📝 Beschreibung:
        ${module.title}
        
        🌐 Webseite:
        ${module.homepageUrl}
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("ℹ️ Modul-Informationen")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("⬇️ Download") { dialog, _ ->
                downloadModuleFromApi(module.downloadurl, module.homepageUrl, module.title)
            }
            .show()
    }
    fun getGitHubApiLatestReleaseUrl(homepageUrl: String): String? {
        val regex = Regex("https://github.com/([^/]+)/([^/]+)(?:\\.git)?/?")
        val matchResult = regex.find(homepageUrl)
        return if (matchResult != null) {
            val (owner, repo) = matchResult.destructured
            "https://api.github.com/repos/$owner/$repo/releases/latest"
        } else {
            println("Regex konnte homepageUrl nicht matchen: $homepageUrl")
            null
        }
    }

    suspend fun getLatestAssetDownloadUrl(apiReleasesUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(apiReleasesUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return@withContext null
            }

            val responseText = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val json = JSONObject(responseText)
            val assets = json.getJSONArray("assets")

            if (assets.length() > 0) {
                // Beispiel: das erste Asset nehmen
                val asset = assets.getJSONObject(0)
                return@withContext asset.getString("browser_download_url")
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun downloadModuleFromApi(apiUrl: String, homepageUrl: String, packedName: String) {

        val selectedLanguage = loadLanguage(this)
        val translations = loadTranslations(this)
        val texts = translations[selectedLanguage] ?: emptyMap()

        CoroutineScope(Dispatchers.Main).launch {
            val fileName = apiUrl.substringAfterLast("/")
            val outputFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

            if (outputFile.exists()) outputFile.delete() // Alte Datei entfernen

            val inflater = LayoutInflater.from(this@MainActivity)
            val dialogView = inflater.inflate(R.layout.dialog_download_progress, null)

            val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
            val progressText = dialogView.findViewById<TextView>(R.id.progressText)
            val installButton = dialogView.findViewById<Button>(R.id.installButton)
            val closeButton = dialogView.findViewById<Button>(R.id.closeButton)

            closeButton.visibility = View.GONE
            installButton.visibility = View.GONE
            installButton.text = texts["Install"] ?: "Install"
            closeButton.text = texts["close"] ?: "Close"

            val titleView = TextView(this@MainActivity).apply {
                text = "${texts["download2"]} $packedName"
                setPadding(40, 40, 40, 20)
                textSize = 18f
                setTextColor(Color.BLUE)
                setTypeface(null, Typeface.BOLD)
            }

            println("Regex konnte homepageUrl nicht matchen: $homepageUrl")
            println("Regex konnte homepageUrl nicht matchen: $apiUrl")
            var downloadName = ""

            val alertDialog = AlertDialog.Builder(this@MainActivity)
                .setCustomTitle(titleView)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            alertDialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.rounded_container2))
            alertDialog.show()

            fun getRandomizedOutputFile(baseFile: File): File {
                val parent = baseFile.parentFile
                val name = baseFile.nameWithoutExtension
                val ext = baseFile.extension

                val randomNumber = Random.nextInt(10000, 99999) // 5-stellige Zufallszahl
                val newName = "${name}_$randomNumber.${ext}"
                downloadName = newName
                return File(parent, newName)
            }

            // Download Funktion im IO-Dispatcher
            suspend fun downloadFile(context: Context, urlString: String): Boolean {
                val randomizedOutputFile = getRandomizedOutputFile(outputFile)

                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "MyApp::DownloadWakeLock")

                var connection: HttpURLConnection? = null
                return try {
                    wakeLock.acquire()

                    val url = URL(urlString)
                    connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connect()

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        return false
                    }

                    val fileLength = connection.contentLength

                    connection.inputStream.use { input ->
                        FileOutputStream(randomizedOutputFile).use { output ->
                            val data = ByteArray(4096)
                            var total: Long = 0
                            var count: Int

                            while (input.read(data).also { count = it } != -1) {
                                output.write(data, 0, count)
                                total += count

                                val progress = if (fileLength > 0) (total * 100 / fileLength).toInt() else -1

                                withContext(Dispatchers.Main) {
                                    if (progress >= 0) {
                                        progressBar.progress = progress
                                        progressText.text = "$progress%"
                                    } else {
                                        progressText.text = "Downloading..."
                                    }
                                }
                            }
                            output.flush()
                        }
                    }

                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                } finally {
                    connection?.disconnect()
                    if (wakeLock.isHeld) wakeLock.release()
                }
            }


            val success = withContext(Dispatchers.IO) {
                if (downloadFile(this@MainActivity, apiUrl)) {
                    true
                } else {
                    val regex = Regex("https://github.com/([^/]+)/([^/]+)(?:\\.git)?/?")
                    val matchResult = regex.find(homepageUrl)
                    if (matchResult != null) {
                        val (owner, repo) = matchResult.destructured
                        val latestAssetUrl = getLatestAssetDownloadUrl("https://api.github.com/repos/$owner/$repo/releases/latest")
                        if (latestAssetUrl != null) {
                            downloadFile(this@MainActivity, latestAssetUrl)
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
            }

            // UI Updates nach Download
            if (success) {
                closeButton.visibility = View.VISIBLE
                installButton.visibility = View.VISIBLE

                closeButton.setOnClickListener { alertDialog.dismiss() }
                installButton.setOnClickListener {
                    installButton.isEnabled = false
                    closeButton.isEnabled = false
                    installButton.visibility = View.GONE
                    closeButton.visibility = View.GONE
                    progressBar.isIndeterminate = true

                    CoroutineScope(Dispatchers.IO).launch {
                        val command = """su -c "mkdir -p /data/local/tmp && cp '/sdcard/Download/${downloadName}' /data/local/tmp/'$downloadName' && pm install /data/local/tmp/'$downloadName' && rm -rf /data/local/tmp/'$downloadName'" """
                        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                        val exitCode = process.waitFor()

                        withContext(Dispatchers.Main) {
                            progressBar.isIndeterminate = false

                            if (exitCode == 0) {
                                installButton.text = texts["open"] ?: "Open"
                                installButton.visibility = View.VISIBLE
                                closeButton.visibility = View.VISIBLE
                                installButton.isEnabled = true
                                closeButton.isEnabled = true

                                if (packedName.isNotEmpty()) {
                                    installButton.setOnClickListener {
                                        try {
                                            // Root-Befehl zur Ermittlung der Startaktivität
                                            val result = Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd package resolve-activity --brief $packedName"))
                                            val output = result.inputStream.bufferedReader().readLines()
                                            val resolvedActivity = output.lastOrNull()?.trim()


                                            if (!resolvedActivity.isNullOrEmpty() && resolvedActivity.contains("/")) {
                                                val cmd = arrayOf("su", "-c", "am start -n $resolvedActivity")
                                                val proc = Runtime.getRuntime().exec(cmd)
                                                val exit = proc.waitFor()

                                                if (exit != 0) {
                                                    // fallback: App-Einstellungen
                                                    val packageUri = Uri.parse("package:$packedName")
                                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                        data = packageUri
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    startActivity(intent)
                                                }
                                            } else {
                                                // fallback: App-Einstellungen
                                                val packageUri = Uri.parse("package:$packedName")
                                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = packageUri
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                startActivity(intent)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            val packageUri = Uri.parse("package:$packedName")
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = packageUri
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            startActivity(intent)
                                        }
                                    }
                                } else {
                                    installButton.visibility = View.GONE
                                }
                                closeButton.setOnClickListener { alertDialog.dismiss() }
                            } else {
                                Toast.makeText(this@MainActivity, "❌ Error: $exitCode", Toast.LENGTH_LONG).show()
                                installButton.isEnabled = true
                                closeButton.isEnabled = true
                                installButton.visibility = View.VISIBLE
                                closeButton.visibility = View.VISIBLE
                                closeButton.setOnClickListener { alertDialog.dismiss() }
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this@MainActivity, texts["download_failed"] ?: "Download failed", Toast.LENGTH_LONG).show()
                alertDialog.dismiss()
                // Optional: Webseite öffnen als letzte Option
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(homepageUrl))
                startActivity(intent)
            }
        }
    }

    private fun installApkWithRoot(filePath: String, fileName: String): Int {
        val command = """su -c "mkdir -p /data/adb/tmp && cp '$filePath' /data/adb/tmp/'$fileName' && pm install /data/adb/tmp/'$fileName'" """
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        return process.waitFor()
    }

    private fun getPackageNameFromApk(apkFile: File): String? {
        val pm = packageManager
        val info = pm.getPackageArchiveInfo(apkFile.path, 0)
        info?.applicationInfo?.sourceDir = apkFile.path
        info?.applicationInfo?.publicSourceDir = apkFile.path
        return info?.packageName
    }

    private fun createDownloadDialog(fileName: String): AlertDialog {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Download $fileName")

        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_download_progress, null)
        builder.setView(dialogView)
        builder.setCancelable(false)

        return builder.create()
    }

    private fun startDownload(downloadUrl: String, fileName: String) {
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("Modul-Download")
            .setDescription("Lade $fileName herunter...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)

        Toast.makeText(this, "⬇️ Download gestartet: $fileName", Toast.LENGTH_SHORT).show()
    }

    private fun getVerifiedBootHash(): String {
        return try {
            val keyPair = createKeyPair()
            val attestationCert = getAttestationCertificate()
            extractAttestationData(attestationCert)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Fehler bei der Attestation", e)
            "Fehler: ${e.message}"
        }
    }

    private fun createKeyPair(): java.security.KeyPair {
        val keyGen = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore"
        )

        val spec = KeyGenParameterSpec.Builder(
            "bootKey",
            KeyProperties.PURPOSE_SIGN
        ).apply {
            setDigests(KeyProperties.DIGEST_SHA256)
            setAttestationChallenge("1234".toByteArray())
            setKeyValidityStart(Date())
        }.build()

        keyGen.initialize(spec)
        return keyGen.generateKeyPair()
    }

    private fun getAttestationCertificate(): X509Certificate {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val certChain = keyStore.getCertificateChain("bootKey")
        if (certChain == null || certChain.isEmpty()) {
            throw Exception("Zertifikatskette nicht gefunden")
        }

        return certChain[0] as X509Certificate
    }

    private fun extractAttestationData(cert: X509Certificate): String {
        return try {
            val oid = "1.3.6.1.4.1.11129.2.1.17"
            val extBytes = cert.getExtensionValue(oid) ?: return ""

            val input1 = ASN1InputStream(extBytes)
            val derObject = input1.readObject() as ASN1OctetString
            input1.close()

            val input2 = ASN1InputStream(derObject.octets)
            val attestationSeq = input2.readObject() as ASN1Sequence
            input2.close()

            if (attestationSeq.size() > 7) {
                val item7 = attestationSeq.getObjectAt(7) as ASN1Sequence

                for (i in 0 until item7.size()) {
                    val str = item7.getObjectAt(i).toString()
                    if ("704" in str && "#" in str) {
                        return str.substringAfterLast("#").trim().removeSuffix("]")
                    }
                }
            }
            ""
        } catch (e: Exception) {
            ""
        }
    }
    fun setbuttonstexts(){
        val selectedLanguage = loadLanguage(this)
        val translations = loadTranslations(this)
        val texts = translations[selectedLanguage] ?: emptyMap()

        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_menu, null)
        val userAppsCheckbox = popupView.findViewById<CheckBox>(R.id.user_apps_checkbox)
        val systemAppsCheckbox = popupView.findViewById<CheckBox>(R.id.system_apps_checkbox)

        texts["app_list_button"]?.let { findViewById<Button>(R.id.app_list_button).text = it }
        texts["setboothash"]?.let { findViewById<Button>(R.id.setboothash).text = it }
        texts["language"]?.let { findViewById<Button>(R.id.selectLangage).text = it }
        texts["install_integrity_button"]?.let { findViewById<Button>(R.id.install_integrity_button).text = it }
        texts["clear_play_service_button"]?.let { findViewById<Button>(R.id.clear_play_service_button).text = it }
        texts["installed_modules_page"]?.let { findViewById<Button>(R.id.installed_modules_page).text = it }
        texts["lsposed_repo"]?.let { findViewById<Button>(R.id.lsposed_repo).text = it }
        texts["btnHideMagisk"]?.let { findViewById<Button>(R.id.btnHideMagisk).text = it }
        texts["btn_select_modules"]?.let { findViewById<Button>(R.id.btn_select_modules).text = it }

        texts["SearchApp"]?.let { findViewById<EditText>(R.id.search_box).setHint(it) }

        texts["CeackBoxUserApps"]?.let { userAppsCheckbox.text = it }
        texts["CeackBoxSystemApps"]?.let { systemAppsCheckbox.text = it }
    }


    private fun showProgressDialog(selectedPackages: List<String>, appTypeFunction: (String) -> Boolean) {
        val selectedLanguage = loadLanguage(this)
        val translations = loadTranslations(this)
        val texts = translations[selectedLanguage] ?: emptyMap()
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.progress_dialog, null)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val progressText = dialogView.findViewById<TextView>(R.id.progressText)

        progressBar.max = selectedPackages.size
        progressBar.progress = 0

        val dialog = AlertDialog.Builder(this)
            .setTitle(texts["Migrate"] ?: "Apps erstellen")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            val results = StringBuilder()

            selectedPackages.forEachIndexed { index, packageName ->

                withContext(Dispatchers.Main) {
                    progressBar.progress = index + 1
                    progressText.text = "${texts["Migrate"] ?: "Verschiebe"}: $packageName"
                }

                val success = appTypeFunction(packageName) // Rufe die gewählte Funktion auf
                results.append(
                    if (success)
                        "$packageName → ${texts["done"] ?: "Fertig"}\n"
                    else
                        "$packageName → ${texts["nodel"] ?: "nicht verschoben"}\n"
                )

                delay(500) // nur zur optischen Verzögerung
            }

            withContext(Dispatchers.Main) {
                dialog.dismiss()
                statusText.text = results.toString()
                Toast.makeText(this@MainActivity, "Neustart wird empfohlen!", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    fun makeSystemApp(packageName: String): Boolean {
        return try {
            // Pfad zur APK holen
            val getPath = Runtime.getRuntime().exec("su -c pm path $packageName")
            val apkPath = getPath.inputStream.bufferedReader().readText()
                .trim().removePrefix("package:")

            if (apkPath.isEmpty()) return false

            val systemAppPath = "/system/priv-app/$packageName/base.apk"

            val commands = listOf(
                "mount -o remount,rw /",
                "mkdir -p /system/priv-app/$packageName",
                "cp $apkPath $systemAppPath",
                "chmod 644 $systemAppPath",
                "chown root:root $systemAppPath"
            )

            val process = Runtime.getRuntime().exec("su")
            val os = process.outputStream.bufferedWriter()

            for (cmd in commands) {
                os.write("$cmd\n")
            }
            os.write("exit\n")
            os.flush()
            os.close()
            process.waitFor()

            // Überprüfen, ob die Datei wirklich im Zielverzeichnis liegt
            val check = Runtime.getRuntime().exec("su -c ls $systemAppPath")
            val checkResult = check.inputStream.bufferedReader().readText().trim()
            val isCopied = checkResult.contains("base.apk")

            if (isCopied) {
                // Ursprüngliche APK löschen
                Runtime.getRuntime().exec("su -c rm -rf ${apkPath.substringBeforeLast("/")}")
            }

            isCopied
        } catch (e: Exception) {
            false
        }
    }

    fun makeUserApp(packageName: String): Boolean {
        return try {
            // Pfad zur APK holen
            val getPath = Runtime.getRuntime().exec("su -c pm path $packageName")
            val apkPath = getPath.inputStream.bufferedReader().readText().trim().removePrefix("package:")

            if (apkPath.isEmpty()) return false

            val userAppPath = "/data/app/$packageName"

            // Sicherstellen, dass das Zielverzeichnis existiert
            val commands = listOf(
                "mount -o remount,rw /",  // Sicherstellen, dass das System schreibbar ist (falls notwendig)
                "mkdir -p $userAppPath",   // Verzeichnis für Benutzer-App erstellen
                "cp $apkPath $userAppPath/base.apk", // Kopieren der APK
                "chmod 644 $userAppPath/base.apk", // Berechtigungen setzen
                "chown system:system $userAppPath/base.apk" // Besitzer setzen
            )

            // Führt die Shell-Befehle aus
            val process = Runtime.getRuntime().exec("su")
            val os = process.outputStream.bufferedWriter()

            for (cmd in commands) {
                os.write("$cmd\n")
            }
            os.write("exit\n")
            os.flush()
            os.close()
            process.waitFor()

            // Überprüfen, ob die Datei wirklich im Zielverzeichnis liegt
            val check = Runtime.getRuntime().exec("su -c ls $userAppPath")
            val checkResult = check.inputStream.bufferedReader().readText().trim()


            val isCopied = checkResult.contains("base.apk")

            if (isCopied) {
                Runtime.getRuntime().exec("su -c mount -o remount,rw /")
                // Ursprüngliche APK löschen (Stelle sicher, dass der Pfad korrekt ist)
                val deleteOriginal = Runtime.getRuntime().exec("su -c rm -rf $apkPath")
                deleteOriginal.waitFor()
            }

            isCopied
        } catch (e: Exception) {
            // Fehlerbehandlung, z. B. Logging
            e.printStackTrace()
            false
        }
    }

    private fun copyAssetsToSdcard(selected: String, onComplete: () -> Unit) {
        // Erstelle Dialog mit ProgressBar
        val dialog = AlertDialog.Builder(this)
            .setTitle("Please Wait…")
            .setCancelable(false) // NICHT schließbar!
            .create()

        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPadding(50, 40, 50, 40)

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progressBar.max = 100
        progressBar.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        linearLayout.addView(progressBar)

        dialog.setView(linearLayout)
        dialog.show()

        // Hintergrund-Thread starten
        Thread {
            try {
                var assetPath = "magisk/magisk_official"
                when (selected) {
                    "Magisk Alpha" -> assetPath = "magisk/magisk_alpha"
                    "Magisk Canery" -> assetPath = "magisk/magisk_canery"
                    "Magisk Kitsune" -> assetPath = "magisk/magisk_kitsune"
                }

                val destPath = "/data/local/tmp/URManager"

                // Alte Daten löschen
                Runtime.getRuntime().exec(arrayOf("su", "-c", "rm -rf $destPath")).waitFor()
                // Zielordner neu anlegen
                Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p $destPath")).waitFor()

                val files = assets.list(assetPath)
                if (files != null) {
                    val totalFiles = files.size
                    var copied = 0

                    for (filename in files) {
                        val input = assets.open("$assetPath/$filename")
                        val tempFile = File.createTempFile("tmp", null, cacheDir)
                        val output = FileOutputStream(tempFile)
                        input.copyTo(output)
                        input.close()
                        output.close()

                        Runtime.getRuntime().exec(arrayOf("su", "-c", "cp ${tempFile.absolutePath} $destPath/$filename")).waitFor()
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "chmod 755 $destPath/$filename")).waitFor()

                        tempFile.delete()
                        copied++

                        val progress = (copied * 100) / totalFiles
                        runOnUiThread {
                            smoothProgressUpdate(progressBar, progress)
                        }
                    }
                }

                runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(this, "✅ Copyed!", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    fun smoothProgressUpdate(progressBar: ProgressBar, target: Int) {
        val current = progressBar.progress
        val step = if (target - current > 5) 2 else 1

        if (current < target) {
            progressBar.progress = current + step
            progressBar.postDelayed({
                smoothProgressUpdate(progressBar, target)
            }, 20) // 10ms Pause zwischen jedem Schritt
        } else {
            progressBar.progress = target
        }
    }

    private fun pickBootImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/octet-stream"
        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val fileName = getFileNameFromUri(uri)

                if (!fileName.endsWith(".img", ignoreCase = true)) {
                    Toast.makeText(this, "Only .img files allowed!", Toast.LENGTH_SHORT).show()
                    return
                }

                val context = this

                // Fortschrittsbalken (horizontal)
                val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 100
                    progress = 0
                    isIndeterminate = false
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val progressText = TextView(context).apply {
                    text = "0%"
                    gravity = Gravity.CENTER
                }

                val layout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(50, 40, 50, 10)
                    addView(progressBar)
                    addView(progressText)
                }

                val dialog = AlertDialog.Builder(context)
                    .setTitle("Copying boot.img")
                    .setView(layout)
                    .setCancelable(false)
                    .create()
                dialog.show()

                Thread {
                    val destFile = File(getExternalFilesDir(null), "boot.img")
                    val success = copyUriToFileWithProgress(context, uri, destFile) { percent ->
                        runOnUiThread {
                            progressBar.progress = percent
                            progressText.text = "$percent%"
                        }
                    }

                    runOnUiThread {
                        dialog.dismiss()
                        if (success) {
                            selectedBootImg = destFile
                            Toast.makeText(context, "Boot image copied: ${destFile.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to copy boot image!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
        }
    }

    fun getFileNameFromUri(uri: Uri): String {
        var name = "unknown.img"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }


    fun copyUriToFileWithProgress(context: Context, uri: Uri, destFile: File, onProgress: (Int) -> Unit): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return false
            val outputStream = FileOutputStream(destFile)
            val buffer = ByteArray(4096)
            val totalSize = context.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: -1
            var bytesCopied: Long = 0
            var lastPercent = 0

            var bytes = inputStream.read(buffer)
            while (bytes >= 0) {
                outputStream.write(buffer, 0, bytes)
                bytesCopied += bytes

                if (totalSize > 0) {
                    val percent = (bytesCopied * 100 / totalSize).toInt()
                    if (percent != lastPercent) {
                        onProgress(percent)
                        lastPercent = percent
                    }
                }

                bytes = inputStream.read(buffer)
            }

            inputStream.close()
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun runBootPatchScript() {
        val scriptFile = File("/data/local/tmp/URManager/boot_patch.sh")

        outputText.text = ""

        runOnUiThread {
            outputText.text = "$magisk_patch_select selected"
        }
        // Vorabprüfung: selectedBootImg initialisiert?
        if (!::selectedBootImg.isInitialized) {
            runOnUiThread {
                outputText.text = "no boot image selected!!!"
            }
            return
        }

        if (!selectedBootImg.toString().endsWith(".img")) {
            runOnUiThread {
                outputText.text = "no image file!!!"
            }
            return
        }

        if (!scriptFile.exists() || !selectedBootImg.exists()) {
            runOnUiThread {
                Toast.makeText(this, "No File! Copy Now", Toast.LENGTH_SHORT).show()
            }
            copyAssetsToSdcard(magisk_patch_select) {

                runOnUiThread {
                    outputText.text = "Start Patch...\n"
                    outputText.append("\nPatch whit $magisk_patch_select \n")
                }

                Thread {
                    try {
                        val cmd = "sh ${scriptFile.absolutePath} ${selectedBootImg.absolutePath}"
                        val processBuilder = ProcessBuilder("su", "-c", cmd)
                        processBuilder.redirectErrorStream(true)

                        val process = processBuilder.start()
                        val reader = BufferedReader(InputStreamReader(process.inputStream))

                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val finalLine = line
                            runOnUiThread {
                                outputText.append("$finalLine\n")
                            }
                        }

                        val exitCode = process.waitFor()

                        val cmd2 = "/data/local/tmp/URManager/magiskboot sign /data/local/tmp/URManager/new-boot.img"
                        val processBuilder2 = ProcessBuilder("su", "-c", cmd2)

                        processBuilder2.redirectErrorStream(true)

                        val process2 = processBuilder2.start()
                        val reader2 = BufferedReader(InputStreamReader(process2.inputStream))

                        var line2: String?
                        while (reader2.readLine().also { line2 = it } != null) {
                            val finalLine = line2
                            runOnUiThread {
                                outputText.append("$finalLine\n")
                            }
                        }

                        val exitCode2 = process2.waitFor()

                        val randomNummer = (1000..9999).random()
                        val fileName = "patched_${magisk_patch_select.replace(" ", "_")}_boot_$randomNummer.img"
                        val output = "/sdcard/Download/$fileName"

                        // Kopieren des neuen Boot-Images
                        try {
                            ProcessBuilder("su", "-c", "cp /data/local/tmp/URManager/new-boot.img \"$output\"")
                                .start().waitFor()
                            runOnUiThread {
                                outputText.append("\n\nBoot-Image saved: $output")
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                outputText.append("copy error: ${e.message}\n")
                            }
                        }

                        runOnUiThread {
                            outputText.append("\nFinish $exitCode")
                        }
                        runOnUiThread {
                            outputText.append("\nFinish $exitCode2")
                        }
                        // Aufräumen
                        try {
                            ProcessBuilder("su", "-c", "rm -r /data/local/tmp/URManager")
                                .start().waitFor()
                        } catch (e: Exception) {
                            runOnUiThread {
                                outputText.append("Clear error: ${e.message}\n")
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        runOnUiThread {
                            outputText.append("Error: ${e.message}\n")
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                        // Fallback: Aufräumen auch bei Exception versuchen
                        try {
                            ProcessBuilder("su", "-c", "rm -r /data/local/tmp/URManager")
                                .start().waitFor()
                        } catch (_: Exception) { /* ignoriere */ }
                    }
                }.start()
            }
            return
        }

        runOnUiThread {
            outputText.text = "Start Patch...\n"
            outputText.append("\nPatch whit $magisk_patch_select \n")
        }

        Thread {
            try {
                val cmd = "sh ${scriptFile.absolutePath} ${selectedBootImg.absolutePath}"
                val processBuilder = ProcessBuilder("su", "-c", cmd)
                processBuilder.redirectErrorStream(true)

                val process = processBuilder.start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val finalLine = line
                    runOnUiThread {
                        outputText.append("$finalLine\n")
                    }
                }

                val exitCode = process.waitFor()


                runOnUiThread {
                    outputText.append("\nBoot Signieren")
                }

                val cmd2 = "/data/local/tmp/URManager/magiskboot sign /data/local/tmp/URManager/new-boot.img"
                val processBuilder2 = ProcessBuilder("su", "-c", cmd2)

                processBuilder2.redirectErrorStream(true)

                val process2 = processBuilder2.start()
                val reader2 = BufferedReader(InputStreamReader(process2.inputStream))

                var line2: String?
                while (reader2.readLine().also { line2 = it } != null) {
                    val finalLine = line2
                    runOnUiThread {
                        outputText.append("$finalLine\n")
                    }
                }

                val exitCode2 = process2.waitFor()

                val randomNummer = (1000..9999).random()
                val fileName = "patched_${magisk_patch_select.replace(" ", "_")}_boot_$randomNummer.img"
                val output = "/sdcard/Download/$fileName"

                // Kopieren des neuen Boot-Images
                try {
                    ProcessBuilder("su", "-c", "cp /data/local/tmp/URManager/new-boot.img \"$output\"")
                        .start().waitFor()
                    runOnUiThread {
                        outputText.append("\n\nBoot-Image saved: $output")
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        outputText.append("copy error: ${e.message}\n")
                    }
                }

                runOnUiThread {
                    outputText.append("\nFinish $exitCode")
                }
                runOnUiThread {
                    outputText.append("\nFinish $exitCode2")
                }



                // Aufräumen
                try {
                    ProcessBuilder("su", "-c", "rm -r /data/local/tmp/URManager")
                        .start().waitFor()
                } catch (e: Exception) {
                    runOnUiThread {
                        outputText.append("Clear error: ${e.message}\n")
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    outputText.append("Error: ${e.message}\n")
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }

                // Fallback: Aufräumen auch bei Exception versuchen
                try {
                    ProcessBuilder("su", "-c", "rm -r /data/local/tmp/URManager")
                        .start().waitFor()
                } catch (_: Exception) { /* ignoriere */ }
            }
        }.start()
    }

    fun showAutoConfigPopup(context: Context) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Automatische Konfiguration")
            .setMessage("Konfiguration läuft...\n\n")
            .setCancelable(false)
            .create()

        dialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            val scriptPath = "sh /data/adb/get_extra.sh --security-patch"
            val processBuilder = ProcessBuilder("su", "-c", scriptPath)

            try {
                val process = processBuilder.start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))

                val outputBuilder = StringBuilder()
                var outputLine: String?
                var notSetFound = false

                while (reader.readLine().also { outputLine = it } != null) {
                    outputBuilder.append(outputLine).append("\n")
                    if (outputLine!!.contains("not set")) {
                        notSetFound = true
                    }
                }

                val exitCode = process.waitFor()

                withContext(Dispatchers.Main) {
                    // Debug-Output im Dialog anzeigen
                }

                if (exitCode == 0 && !notSetFound) {
                    // Datei mit su erstellen
                    val command = "mkdir -p /data/adb/tricky_store && touch /data/adb/tricky_store/security_patch_auto_config"
                    val suProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                    val suExitCode = suProcess.waitFor()

                    withContext(Dispatchers.Main) {
                        if (suExitCode == 0) {
                        } else {
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                    }
                }

                // Hier kannst du loadCurrentConfig() aufrufen, falls nötig

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                }
            }
        }
    }
    private fun showWhatsappInstalledDialog(
        dialogText: TextView,
        rebootButton: Button,
        continueButton: Button,
        dialog: AlertDialog
    ) {
        val selectedLanguage = loadLanguage(this)
        val translations = loadTranslations(this)
        val texts = translations[selectedLanguage] ?: emptyMap()
        val spannable = SpannableString(texts["whatsappisinstalled"])

        spannable.setSpan(
            ForegroundColorSpan(Color.YELLOW),
            0,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        runOnUiThread {
            dialogText.append(spannable)
        }


        // Buttons konfigurieren
        rebootButton.apply {
            text = texts["Cancle"]
            isEnabled = true
            alpha = 1f
            setOnClickListener {
                dialog.dismiss()
            }
        }

        continueButton.apply {
            text = texts["delwhatsapp"]
            isEnabled = true
            alpha = 1f
            setOnClickListener {
                fixwhatsapp(dialogText, rebootButton, continueButton, dialog)
            }
        }
    }

    fun fixwhatsapp(
        dialogText: TextView,
        rebootButton: Button,
        continueButton: Button,
        dialog: AlertDialog
    ) {
        val selectedLanguage = loadLanguage(this)
        val translations = loadTranslations(this)
        val texts = translations[selectedLanguage] ?: emptyMap()
        val modulelist = mutableListOf<String>()
        val modulelistPath = mutableListOf<String>()
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
        progressBar.max = 100
        progressBar.progress = 0
        progressBar.visibility = View.VISIBLE

        // Buttons konfigurieren
        rebootButton.apply {
            text = texts["restart"]
            isEnabled = false
            alpha = 0.5f
            setOnClickListener {
                dialog.dismiss()
            }
        }

        continueButton.apply {
            text = texts["confirm"]
            isEnabled = false
            alpha = 0.5f
            setOnClickListener {
                fixwhatsapp(dialogText, rebootButton, continueButton, dialog)
            }
        }

        dialogText.append("\n${texts["uninstallWhatsapp"]}" )

        Thread {
            var deinstallWhatsapp = RootUtils.runCommand("pm uninstall com.whatsapp")
            val folderPath = File("/sdcard/Download/FixIntegrity")
            val destinationFolder = folderPath // für Module Download
            val downloader = Downloader(this)

            if (!folderPath.exists()) {
                folderPath.mkdirs()
            }
            if (deinstallWhatsapp == 0) {
                runOnUiThread { dialogText.append("\n${texts["whatsappUninstalled"]}") }
            }
            runOnUiThread { dialogText.append("\n${texts["whatsappDownloadStart"]}") }

            // Whatsapp APK runterladen
            downloader.downloadFile(
                "https://github.com/Escape000-bit/Secure-Folder/releases/download/1.1/whatsapp.apk",
                File(folderPath, "whatsapp.apk"),
                object : Downloader.DownloadListener {
                    override fun onProgress(message: String) {
                        runOnUiThread {
                            if (message.startsWith("Fortschritt:")) {
                                val percent =
                                    message.removePrefix("Fortschritt:").trim().toIntOrNull() ?: 0
                                progressBar.progress = percent
                            }
                        }
                    }

                    override fun onFinished(success: Boolean) {
                        runOnUiThread {
                            progressBar.progress = if (success) 100 else 0
                            dialogText.append(if (success) "\n${texts["whatsappDownloadSuccess"]}" else "\n${texts["whatsappDownloadFailed"]}")
                        }

                        if (success) {
                            val installCmd = """
                                mkdir -p "/data/local/tmp" && \ 
                                cp "/sdcard/Download/FixIntegrity/whatsapp.apk" "/data/local/tmp/whatsapp.apk" && \
                                pm install "/data/local/tmp/whatsapp.apk" && \
                                rm "/data/local/tmp/whatsapp.apk"
                                """.trimIndent()
                            val installWhatsapp = RootUtils.runCommand(installCmd)


                            listOf(
                                "com.whatsapp",
                                "com.whatsapp com.whatsapp:app_restart",
                                "com.whatsapp com.whatsapp:account_switching"
                            ).forEach { RootUtils.runCommand("magisk --denylist add $it") }

                            runOnUiThread {
                                if (installWhatsapp == 0) {
                                    dialogText.append("\n${texts["whatsappInstallSuccess"]}")
                                    dialogText.append("\n${texts["moduleDownloadStart"]}")
                                } else {
                                    dialogText.append("\n${texts["whatsappInstallFailed"]}")
                                    progressBar.visibility = View.GONE
                                    rebootButton.isEnabled = true
                                    rebootButton.alpha = 1f
                                    continueButton.isEnabled = true
                                    continueButton.alpha = 1f
                                    return@runOnUiThread
                                }
                            }

                            runOnUiThread { dialogText.append("\n${texts["checkModulesToInstall"]}") }
                            val moduleNames = listOf(
                                "BetterKnownInstalled",
                                "playcurlNEXT",
                                "playintegrityfix",
                                "tricky_store",
                                "zn_magisk_compat",
                                "zygisk_lsposed",
                                "zygisk-assistant",
                                "zygisk_shamiko",
                            )

                            val moduleApiUrls = mapOf(
                                "BetterKnownInstalled" to "https://api.github.com/repos/Pixel-Props/BetterKnownInstalled/releases/latest",
                                "playcurlNEXT" to "https://api.github.com/repos/daboynb/playcurlNEXT/releases/latest",
                                "playintegrityfix" to "https://api.github.com/repos/chiteroman/PlayIntegrityFix/releases/latest",
                                "tricky_store" to "https://api.github.com/repos/5ec1cff/TrickyStore/releases/latest",
                                "zn_magisk_compat" to "https://api.github.com/repos/Dr-TSNG/ZygiskNext/releases/latest",
                                "zygisk_lsposed" to "https://api.github.com/repos/LSPosed/LSPosed/releases/latest",
                                "zygisk-assistant" to "https://api.github.com/repos/snake-4/Zygisk-Assistant/releases/latest",
                                "zygisk_shamiko" to "https://api.github.com/repos/LSPosed/Shamiko/releases/latest",
                            )

                            val lsposed = RootUtils.runCommand("ls /data/adb/modules/zygisk_lsposed") == 0

                            if (lsposed) {
                                runOnUiThread {
                                    dialogText.append("\n${texts["lsposedInstalled"]}")
                                }
                            } else {
                                runOnUiThread { dialogText.append("\n${texts["lsposedNotInstalled"]}") }
                                copyAssetToPath(this@MainActivity, "LSPosed.zip", "/sdcard/download/FixIntegrity/lsposed.zip")
                                val installer = RootUtils.runCommandWithOutput("magisk --install-module /sdcard/download/FixIntegrity/lsposed.zip")
                                runOnUiThread { dialogText.append("\nLSPOSED:\n$installer") }
                            }

                            for (module in moduleNames) {
                                val installed =
                                    RootUtils.runCommand("ls /data/adb/modules/$module") == 0
                                if (installed) {
                                    runOnUiThread {
                                        dialogText.append("\n$module ${texts["moduleAlreadyInstalled"]}")
                                    }
                                } else {
                                    runOnUiThread { dialogText.append("\n$module ${texts["moduleNotInstalled"]}") }
                                    modulelist.add(moduleApiUrls[module].toString())
                                }
                            }
                            runOnUiThread { dialogText.append("\n${texts["checkDataEncryption"]}") }

                            val cryptoState = Runtime.getRuntime().exec(arrayOf("su", "-c", "getprop ro.crypto.state"))
                                .inputStream.bufferedReader().readText().trim()

                            if (cryptoState == "unencrypted") {
                                runOnUiThread { dialogText.append("\n${texts["dataNotEncrypted"]}") }
                                modulelist.add("https://api.github.com/repos/ThePieMonster/ZygiskFakeEncryption/releases/latest")
                            }
                            else{
                                runOnUiThread { dialogText.append("\n${texts["dataEncrypted"]}") }
                            }
                            // Module nacheinander downloaden
                            Thread {
                                val client = OkHttpClient()
                                var totalModules = modulelist.size
                                var completedModules = 0

                                for (apiUrl in modulelist) {
                                    try {
                                        val request = Request.Builder().url(apiUrl).build()
                                        val response = client.newCall(request).execute()

                                        if (!response.isSuccessful) {
                                            runOnUiThread {
                                                dialogText.append("\n${texts["errorFetching"]} $apiUrl")
                                            }
                                            completedModules++
                                            continue
                                        }

                                        val body = response.body?.string() ?: ""
                                        val json = JSONObject(body)

                                        // Repo Name optional
                                        val repoName = json.optString("name", "Unbekannt")

                                        val assets = json.getJSONArray("assets")

                                        var downloadUrl: String? = null
                                        var fileName: String? = null
                                        for (i in 0 until assets.length()) {
                                            val asset = assets.getJSONObject(i)
                                            val name = asset.getString("name")
                                            if (name.endsWith(".zip") || name.endsWith(".apk")) {
                                                downloadUrl = asset.getString("browser_download_url")
                                                fileName = name
                                                break
                                            }
                                        }

                                        if (downloadUrl == null || fileName == null) {
                                            runOnUiThread {
                                                dialogText.append("\n${texts["noDownloadAssetFound"]} $repoName")
                                            }
                                            completedModules++
                                            continue
                                        }

                                        runOnUiThread {
                                            dialogText.append("\n${texts["startDownload"]} $fileName")
                                        }

                                        val destinationFile = File(destinationFolder, fileName)

                                        val downloadSuccess = downloader.downloadFile(downloadUrl, destinationFile, object : Downloader.DownloadListener {
                                            override fun onProgress(message: String) {
                                                runOnUiThread {
                                                    if (message.startsWith("Fortschritt:")) {
                                                        val percent =
                                                            message.removePrefix("Fortschritt:").trim().toIntOrNull() ?: 0
                                                        // Fortschritt auf Module anteilig zur GesamtprogressBar (optional)
                                                        val totalProgress = (completedModules * 100 + percent) / totalModules
                                                        progressBar.progress = totalProgress
                                                    }
                                                }
                                            }
                                            override fun onFinished(success: Boolean) {
                                                runOnUiThread {
                                                    if (success) {
                                                        dialogText.append("\n$fileName ${texts["downloadSuccess"]}")
                                                        modulelistPath.add(destinationFile.toString())
                                                    } else {
                                                        dialogText.append("\n${texts["downloadFailed"]} $fileName.")
                                                    }
                                                }
                                            }
                                        })

                                        // Warte auf Download Ende, wenn Downloader synchron/blockierend ist
                                        // Falls Downloader asynchron ist, müsste man hier mit Callbacks arbeiten
                                        // Zum Beispiel eine Synchronisation mit CountDownLatch oder anderen Mechanismen

                                        completedModules++

                                    } catch (e: Exception) {
                                        runOnUiThread {
                                            dialogText.append("\n${texts["moduleDownloadError"]} ${e.message}")
                                        }
                                        completedModules++
                                    }
                                }
                                // Alle Downloads fertig, Buttons freigeben
                                runOnUiThread {
                                    progressBar.visibility = View.GONE
                                    rebootButton.isEnabled = true
                                    rebootButton.alpha = 1f
                                    continueButton.isEnabled = true
                                    continueButton.alpha = 1f
                                    dialogText.append("\n${texts["allModulesDownloaded"]}")
                                }

                                for (module in modulelistPath) {
                                    val installer = RootUtils.runCommandWithOutput("magisk --install-module '$module'")
                                    runOnUiThread {
                                        dialogText.append("$installer")
                                    }
                                }
                                runOnUiThread {
                                    dialogText.append("\n${texts["allModulesInstalled"]}")
                                }
                                runOnUiThread {
                                    dialogText.append("\n\n${texts["setWhatsappDenylist"]}")
                                }
                                listOf(
                                    "com.whatsapp",
                                    "com.whatsapp com.whatsapp:app_restart",
                                    "com.whatsapp com.whatsapp:account_switching"
                                ).forEach {
                                    var o = RootUtils.runCommand("magisk --denylist add $it") == 0

                                    if (o) {
                                        runOnUiThread { dialogText.append("✅") }
                                    }
                                }

                                runOnUiThread { dialogText.append("\n\n${texts["setWhatsappTrickyStore"]}") }
                                var o = RootUtils.runCommand("echo com.whatsapp! >> /data/adb/tricky_store/target.txt") == 0
                                if (o) {
                                    runOnUiThread { dialogText.append("\n✅") }
                                }

                                runOnUiThread { dialogText.append("\n${texts["spoofBootHashIfNeeded"]}") }

                                fun getCurrentBootHash(): String {
                                    return try {
                                        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "getprop ro.boot.vbmeta.digest"))
                                        val output = process.inputStream.bufferedReader().readText().trim()
                                        process.waitFor()
                                        output // kann leer sein, aber nie null
                                    } catch (e: Exception) {
                                        "" // Fehler → leeren String zurückgeben
                                    }
                                }

                                val result = getVerifiedBootHash()
                                val currentHash = getCurrentBootHash()

                                if (currentHash != result) {
                                    writeBootHashScript(result)
                                } else {
                                    runOnUiThread { dialogText.append("\n${texts["bootHashAlreadyCorrect"]}") }
                                }
                                runOnUiThread { dialogText.append("\n${texts["copyPifAndKeybox"]}") }
                                copyAssetToPath(this@MainActivity, "pif.json", "/data/adb/pif.json")
                                copyAssetToPath(this@MainActivity, "keybox.xml", "/data/adb/tricky_store/keybox.xml")
                                copyAssetToPath(this@MainActivity, "get_extra.sh", "/data/adb/get_extra.sh")

                                val scriptPath = "sh /data/adb/get_extra.sh --security-patch"
                                val processBuilder = ProcessBuilder("su", "-c", scriptPath)

                                try {
                                    val process = processBuilder.start()
                                    val reader = BufferedReader(InputStreamReader(process.inputStream))

                                    val outputBuilder = StringBuilder()
                                    var outputLine: String?
                                    var notSetFound = false

                                    while (reader.readLine().also { outputLine = it } != null) {
                                        outputBuilder.append(outputLine).append("\n")
                                        if (outputLine!!.contains("not set")) {
                                            notSetFound = true
                                        }
                                    }

                                    val exitCode = process.waitFor()
                                    runOnUiThread {
                                        // Show debug output in dialog
                                        dialogText.append("\n\nDebug Output:\n$outputBuilder")
                                    }

                                    if (exitCode == 0 && !notSetFound) {
                                        // Create file with su permissions
                                        val command = "mkdir -p /data/adb/tricky_store && touch /data/adb/tricky_store/security_patch_auto_config"
                                        val suProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                                        val suExitCode = suProcess.waitFor()

                                        runOnUiThread {
                                            if (suExitCode == 0) {
                                                dialogText.append("Automatic configuration successful")
                                            } else {
                                                dialogText.append("Error creating file with root permissions")
                                            }
                                        }
                                    } else {
                                        runOnUiThread {
                                            dialogText.append("Failed: Security patch not set")
                                        }
                                    }
                                } catch (e: Exception) {
                                    runOnUiThread {
                                        dialogText.append("Error executing command: ${e.message}")
                                    }
                                }
                                runOnUiThread { dialogText.append("\n\n${texts["denylistSet"]}") }
                                val shellCommandsForAdd = listOf(
                                    "com.android.vending com.android.vending",
                                    "com.android.vending com.android.vending:background",
                                    "com.android.vending com.android.vending:instant_app_installer",
                                    "com.android.vending com.android.vending:quick_launch",
                                    "com.android.vending com.android.vending:recovery_mode",
                                    "isolated com.android.vending:com.google.android.finsky.verifier.apkanalysis.service.ApkContentsScanService",
                                    "com.google.android.gms com.google.android.gms",
                                    "com.google.android.gms com.google.android.gms.feedback",
                                    "com.google.android.gms com.google.android.gms.learning",
                                    "com.google.android.gms com.google.android.gms.persistent",
                                    "com.google.android.gms com.google.android.gms.remapping1",
                                    "com.google.android.gms com.google.android.gms.room",
                                    "com.google.android.gms com.google.android.gms.ui",
                                    "com.google.android.gms com.google.android.gms.unstable",
                                    "com.google.android.gms com.google.android.gms:car",
                                    "com.google.android.gms com.google.android.gms:identitycredentials",
                                    "com.google.android.gms com.google.android.gms:snet",
                                    "isolated com.google.android.gms:com.google.android.gms.chimera.IsolatedBoundBrokerService"
                                ).forEach {
                                    var o = RootUtils.runCommand("magisk --denylist add $it") == 0
                                    if (o) {
                                        runOnUiThread { dialogText.append("\nSeted: $it") }
                                    }
                                }
                                runOnUiThread { dialogText.append("\n${texts["cleanup"]}") }
                                RootUtils.runCommand("rm -f /sdcard/Download/FixIntegrity")
                            }.start()

                        } else {
                            // Download Whatsapp fehlgeschlagen
                            runOnUiThread {
                                progressBar.visibility = View.GONE
                                rebootButton.isEnabled = true
                                rebootButton.alpha = 1f
                                continueButton.isEnabled = true
                                continueButton.alpha = 1f
                            }
                        }
                    }
                })

            runOnUiThread {
                progressBar.visibility = View.GONE
                rebootButton.isEnabled = true
                rebootButton.alpha = 1f
                continueButton.isEnabled = true
                continueButton.setOnClickListener { dialog.dismiss() }
                continueButton.alpha = 1f
            }
        }.start()
    }

    fun showUpdateDialog(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.update_urmanager, null)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val installButton = dialogView.findViewById<Button>(R.id.installButton)
        val selectedLanguage = loadLanguage(this)
        val translations = loadTranslations(this)
        val texts = translations[selectedLanguage] ?: emptyMap()
        installButton.isEnabled = false
        installButton.alpha = 0.30f


        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.show()

        Thread {
            try {
                val url = URL("https://api.github.com/repos/Escape000-bit/URManager/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)

                    val versionTag = json.getString("tag_name").removePrefix("v")
                    val latestVersion = versionTag.toDoubleOrNull()

                    if (latestVersion != null && latestVersion > CURRENT_VERSION) {
                        val assets = json.getJSONArray("assets")
                        var downloadUrl: String? = null
                        var apkFileName = "URManager.apk"

                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.getString("name").endsWith(".apk")) {
                                downloadUrl = asset.getString("browser_download_url")
                                val originalName = asset.getString("name").removeSuffix(".apk")
                                val randomNumber = (1000..9999).random()
                                apkFileName = "${originalName}_$randomNumber.apk"
                                break
                            }
                        }

                        if (downloadUrl != null) {
                            val destination = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), apkFileName)

                            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                                .setDestinationUri(Uri.fromFile(destination))
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)


                            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            val downloadId = downloadManager.enqueue(request)

                            var downloadSuccess = false

                            Thread {
                                var downloading = true
                                while (downloading) {
                                    val query = DownloadManager.Query().setFilterById(downloadId)
                                    val cursor = downloadManager.query(query)

                                    if (cursor != null && cursor.moveToFirst()) {
                                        val totalBytes = cursor.getLong(
                                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                        )
                                        val downloadedBytes = cursor.getLong(
                                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                        )

                                        if (totalBytes > 0) {
                                            val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                                            (context as Activity).runOnUiThread {
                                                progressBar.progress = progress
                                            }
                                        }

                                        val status = cursor.getInt(
                                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                                        )
                                        when (status) {
                                            DownloadManager.STATUS_SUCCESSFUL -> {
                                                downloading = false
                                                downloadSuccess = true
                                                (context as Activity).runOnUiThread {
                                                    installButton.isEnabled = true
                                                    installButton.alpha = 1f
                                                    installButton.text = texts["install"]
                                                }
                                            }
                                            DownloadManager.STATUS_FAILED -> {
                                                downloading = false
                                                downloadSuccess = false
                                                (context as Activity).runOnUiThread {
                                                    installButton.isEnabled = true
                                                    installButton.alpha = 1f
                                                    installButton.text = texts["close"]
                                                }
                                            }
                                        }
                                    }
                                    cursor?.close()
                                    Thread.sleep(500)
                                }
                            }.start()

                            installButton.setOnClickListener {

                                val command = """
                                    mkdir -p /data/local/tmp && \
                                    cp ${destination.absolutePath} /data/local/tmp/${destination.name} && \
                                    pm install -r /data/local/tmp/${destination.name}
                                """.trimIndent()

                                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                                val exitCode = process.waitFor()

                                if (exitCode == 0) {
                                } else {
                                    runOnUiThread {
                                        installButton.isEnabled = true
                                        installButton.alpha = 1f
                                        installButton.text = texts["close"]
                                        installButton.setOnClickListener {
                                            dialog.dismiss()
                                        }
                                    }
                                    Toast.makeText(context, "Error: $exitCode)", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } else {
                        // Keine neue Version vorhanden
                        (context as Activity).runOnUiThread {
                            dialog.dismiss()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                (context as Activity).runOnUiThread {
                    installButton.isEnabled = true
                    installButton.alpha = 1f
                    installButton.text = texts["close"]
                    installButton.setOnClickListener {
                        dialog.dismiss()
                    }
                }
            }
        }.start()
    }

    private fun loadLanguagesFromAssets() {
        languagecontainer.removeAllViews()

        val jsonStr = assets.open("translations.json").use {
            BufferedReader(InputStreamReader(it)).readText()
        }

        val jsonObject = JSONObject(jsonStr)
        val languageNames = jsonObject.keys()

        while (languageNames.hasNext()) {
            val lang = languageNames.next()
            val button = Button(this).apply {
                setBackgroundResource(R.drawable.rounded_container2)
                text = lang
                setOnClickListener {
                    showSection(homeContainer)
                    saveLanguage(this@MainActivity, lang)
                    setbuttonstexts()
                }
            }
// LayoutParams mit Margins setzen
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 12, 0, 12) // oben und unten 12dp Abstand, links und rechts 0
            }

            button.layoutParams = params
            button.setTextColor(Color.WHITE)

            languagecontainer.addView(button)
        }
    }

}


