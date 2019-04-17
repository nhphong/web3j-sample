package com.codelink.web3jsample

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_make_transaction.view.*
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.Web3jFactory
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger


class MainActivity : AppCompatActivity() {

  private lateinit var walletFile: File
  private lateinit var web3j: Web3j
  private lateinit var adapter: WalletAdapter

  private val disposables = CompositeDisposable()
  private var wallets = listOf<Wallet>()
  private var toast: Toast? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    walletFile = File("${filesDir.absolutePath}/wallets")
    if (!walletFile.exists() || !walletFile.isDirectory) {
      walletFile.mkdirs()
    }

    web3j = Web3jFactory.build(
      HttpService(
        "https://rinkeby.infura.io/v3/ccbfbd9d1b1c4a31845332512941919c"
      )
    )

    adapter = WalletAdapter()
    with(recyclerView) {
      adapter = this@MainActivity.adapter
      layoutManager = LinearLayoutManager(this@MainActivity, VERTICAL, false)
    }

    generateWalletButton.setOnClickListener {
      generateWallet()
    }

    importWalletButton.setOnClickListener {
      showDialogToImportWallet()
    }

    sendEthButton.setOnClickListener {
      showDialogToMakeTransaction()
    }

    deleteAllWalletsButton.setOnClickListener {
      deleteAllWallets()
    }

    loadWallets()
  }

  override fun onDestroy() {
    disposables.clear()
    super.onDestroy()
  }

  private fun loadWallets() {
    execute({
      val walletPaths = walletFile.listFiles()?.map { it.absolutePath } ?: emptyList()
      walletPaths.map { path ->
        val credential = WalletUtils.loadCredentials(
          DEFAULT_PASSWORD,
          path
        )
        Wallet(
          credential.address,
          getBalance(credential.address),
          credential.ecKeyPair.privateKey.toString(16)
        )
      }
    }) {
      adapter.wallets = it
      wallets = it
    }
  }

  private fun generateWallet() {
    execute({
      WalletUtils.generateLightNewWalletFile(DEFAULT_PASSWORD, walletFile)
    }) {
      loadWallets()
    }
  }

  private fun deleteAllWallets() {
    execute({
      walletFile.deleteRecursively()
      walletFile.mkdirs()
      wallets = emptyList()
    }) {
      loadWallets()
    }
  }

  private fun importWallet(privateKey: String) {
    execute({
      WalletUtils.generateWalletFile(
        DEFAULT_PASSWORD,
        ECKeyPair.create(BigInteger(privateKey, 16)),
        walletFile,
        false
      )
    }) {
      loadWallets()
    }
  }

  private fun makeTransaction(amount: String, from: String, to: String) {
    showMessage(getString(R.string.be_patient))
    execute({
      val credentials = WalletUtils.loadCredentials(
        DEFAULT_PASSWORD,
        walletFile.listFiles { _, name ->
          name.contains(from.substring(2).toLowerCase())
        }.first()
      )

      Transfer.sendFunds(
        web3j, credentials, to,
        BigDecimal(amount),
        Convert.Unit.ETHER
      ).sendAsync().get()
    }) {
      loadWallets()
    }
  }

  private fun getBalance(address: String): String {
    val balance = web3j.ethGetBalance(
      address,
      DefaultBlockParameterName.LATEST
    ).sendAsync().get()

    val tokenValue = Convert.fromWei(balance.balance.toString(), Convert.Unit.ETHER)
    return tokenValue.toString()
  }

  private fun showDialogToImportWallet() {
    val privateKeyEditText = EditText(this).apply {
      hint = "Private Key"
    }

    AlertDialog.Builder(this)
      .setView(privateKeyEditText)
      .setNegativeButton("Cancel") { _, _ -> }
      .setPositiveButton("OK") { _, _ ->
        val privateKey = privateKeyEditText.text.toString()
        if (privateKey.isNotEmpty()) {
          importWallet(privateKey)
        }
      }.show()
  }

  private fun showDialogToMakeTransaction() {
    val view = LayoutInflater.from(this).inflate(R.layout.dialog_make_transaction, null)
    with(view) {
      suggestFromButton.setOnClickListener {
        suggestWalletAddress(fromEditText)
      }
      suggestToButton.setOnClickListener {
        suggestWalletAddress(toEditText)
      }
    }

    AlertDialog.Builder(this)
      .setView(view)
      .setNegativeButton("Cancel") { _, _ -> }
      .setPositiveButton("OK") { _, _ ->
        val amount = view.ethAmount.text.toString()
        val from = view.fromEditText.text.toString()
        val to = view.toEditText.text.toString()
        if (amount.isNotEmpty() && from.isNotEmpty() && to.isNotEmpty()) {
          makeTransaction(amount, from, to)
        }
      }.show()
  }

  private fun suggestWalletAddress(textView: TextView) {
    val addresses = wallets.map { it.address }
    val currentAddress = textView.text.toString()

    if (addresses.contains(currentAddress)) {
      val index = addresses.indexOf(currentAddress)
      textView.text = addresses[(index + 1) % addresses.size]
    } else if (addresses.isNotEmpty()) {
      textView.text = addresses[0]
    }
  }

  private fun <T> execute(
    backgroundJob: () -> T,
    foreGroundJob: ((T) -> Unit)? = null
  ) {
    Single.fromCallable { backgroundJob.invoke() }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({
        foreGroundJob?.invoke(it)
      }, {
        showMessage("${it::class.java.simpleName}{${it.message}}")
      }).let {
        disposables.add(it)
      }
  }

  private fun showMessage(message: String) {
    toast?.cancel()
    toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
    toast?.show()
  }

  companion object {
    const val DEFAULT_PASSWORD = "abc"
  }
}
