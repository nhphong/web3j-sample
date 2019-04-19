package com.codelink.web3jsample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.codelink.web3jsample.adapter.ContractAdapter
import com.codelink.web3jsample.adapter.WalletAdapter
import com.codelink.web3jsample.blockchain.Greeter
import com.codelink.web3jsample.data.Contract
import com.codelink.web3jsample.data.Wallet
import com.codelink.web3jsample.db.Caching
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Transfer
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger


class MainActivity : AppCompatActivity() {

  private lateinit var walletFile: File
  private lateinit var web3j: Web3j
  private lateinit var walletAdapter: WalletAdapter
  private lateinit var contractAdapter: ContractAdapter

  private var wallets = listOf<Wallet>()
  private val caching = Caching(this)
  private val disposables = CompositeDisposable()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupBouncyCastle()
    setContentView(R.layout.activity_main)
    walletFile = File("${filesDir.absolutePath}/wallets")
    if (!walletFile.exists() || !walletFile.isDirectory) {
      walletFile.mkdirs()
    }

    web3j = Web3j.build(
      HttpService(
        "https://rinkeby.infura.io/v3/ccbfbd9d1b1c4a31845332512941919c"
      )
    )

    walletAdapter = WalletAdapter()
    with(walletsRecyclerView) {
      adapter = this@MainActivity.walletAdapter
      layoutManager = LinearLayoutManager(this@MainActivity, VERTICAL, false)
    }

    contractAdapter = ContractAdapter()
    with(contractsRecyclerView) {
      adapter = this@MainActivity.contractAdapter
      layoutManager = LinearLayoutManager(this@MainActivity, VERTICAL, false)
    }

    generateWalletButton.setOnClickListener { generateWallet() }
    importWalletButton.setOnClickListener { showDialogToImportWallet(this, ::importWallet) }
    clearWalletsButton.setOnClickListener { clearWallets() }
    clearContractsButton.setOnClickListener { clearContracts() }
    deployContractButton.setOnClickListener {
      showDialogToDeployContract(
        this,
        wallets,
        ::deployContract
      )
    }
    sendEthButton.setOnClickListener {
      showDialogToMakeTransaction(
        this,
        wallets,
        ::makeTransaction
      )
    }
    loadWallets()
    loadContracts()
  }

  override fun onDestroy() {
    disposables.clear()
    super.onDestroy()
  }

  private fun deployContract(walletAddress: String) {
    showMessage(this, getString(R.string.deploying_contracts))
    execute({
      val credentials = WalletUtils.loadCredentials(
        DEFAULT_PASSWORD,
        walletFile.listFiles { _, name ->
          name.contains(walletAddress.substring(2).toLowerCase())
        }.first()
      )

      val contract = Greeter.deploy(
        web3j,
        credentials,
        DefaultGasProvider(),
        "Have a nice day"
      ).send()

      caching.saveContract(
        Contract(
          contract.contractAddress,
          contract.balance.send().toString(),
          contract.owner.send()
        )
      )
    }) {
      loadContracts()
    }
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
      walletAdapter.wallets = it
      wallets = it
    }
  }

  private fun loadContracts() {
    contractAdapter.contracts = caching.loadContracts()
  }

  private fun generateWallet() {
    execute({
      WalletUtils.generateLightNewWalletFile(DEFAULT_PASSWORD, walletFile)
    }) {
      loadWallets()
    }
  }

  private fun clearWallets() {
    execute({
      walletFile.deleteRecursively()
      walletFile.mkdirs()
      wallets = emptyList()
    }) {
      loadWallets()
    }
  }

  private fun clearContracts() {
    execute({
      caching.clearContracts()
    }) {
      loadContracts()
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
    showMessage(this, getString(R.string.making_transaction))
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
        showMessage(this, "${it::class.java.simpleName}{${it.message}}")
      }).let {
        disposables.add(it)
      }
  }
}
