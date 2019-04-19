package com.codelink.web3jsample

import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.codelink.web3jsample.data.Wallet
import kotlinx.android.synthetic.main.dialog_deploy_contract.view.*
import kotlinx.android.synthetic.main.dialog_make_transaction.view.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

const val DEFAULT_PASSWORD = "abc"

fun setupBouncyCastle() {
  val provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
  if (provider == null) {
    // Web3j will set up the provider lazily when it's first used.
    return
  }

  if (provider is BouncyCastleProvider) {
    // BC with same package name, shouldn't happen in real life.
    return
  }

  // Android registers its own BC provider. As it might be outdated and might not include
  // all needed ciphers, we substitute it with a known BC bundled in the app.
  // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
  // of that it's possible to have another BC implementation loaded in VM.
  Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
  Security.insertProviderAt(BouncyCastleProvider(), 1)
}

fun showDialogToImportWallet(context: Context, importWallet: (String) -> Unit) {
  val privateKeyEditText = EditText(context).apply {
    hint = "Private Key"
  }

  AlertDialog.Builder(context)
    .setView(privateKeyEditText)
    .setNegativeButton("Cancel") { _, _ -> }
    .setPositiveButton("OK") { _, _ ->
      val privateKey = privateKeyEditText.text.toString()
      if (privateKey.isNotEmpty()) {
        importWallet(privateKey)
      }
    }.show()
}

fun showDialogToMakeTransaction(
  context: Context,
  wallets: List<Wallet>,
  makeTransaction: (String, String, String) -> Unit
) {
  val view = LayoutInflater.from(context).inflate(R.layout.dialog_make_transaction, null)
  with(view) {
    suggestFromButton.setOnClickListener {
      suggestWalletAddress(fromEditText, wallets)
    }
    suggestToButton.setOnClickListener {
      suggestWalletAddress(toEditText, wallets)
    }
  }

  AlertDialog.Builder(context)
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

fun showDialogToDeployContract(
  context: Context,
  wallets: List<Wallet>,
  deployContract: (String) -> Unit
) {
  val view = LayoutInflater.from(context).inflate(R.layout.dialog_deploy_contract, null)
  with(view) {
    suggestButton.setOnClickListener {
      suggestWalletAddress(walletAddress, wallets)
    }
  }

  AlertDialog.Builder(context)
    .setView(view)
    .setNegativeButton("Cancel") { _, _ -> }
    .setPositiveButton("OK") { _, _ ->
      val walletAddress = view.walletAddress.text.toString()
      if (walletAddress.isNotEmpty()) {
        deployContract(walletAddress)
      }
    }.show()
}

fun suggestWalletAddress(textView: TextView, wallets: List<Wallet>) {
  val addresses = wallets.map { it.address }
  val currentAddress = textView.text.toString()

  if (addresses.contains(currentAddress)) {
    val index = addresses.indexOf(currentAddress)
    textView.text = addresses[(index + 1) % addresses.size]
  } else if (addresses.isNotEmpty()) {
    textView.text = addresses[0]
  }
}

private var toast: Toast? = null
fun showMessage(context: Context, message: String) {
  toast?.cancel()
  toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
  toast?.show()
}
