package com.codelink.web3jsample.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.codelink.web3jsample.R
import com.codelink.web3jsample.data.Wallet
import kotlinx.android.synthetic.main.item_wallet.view.*

class WalletAdapter : RecyclerView.Adapter<WalletAdapter.WalletHolder>() {

  var wallets = listOf<Wallet>()
    set(value) {
      field = value
      notifyDataSetChanged()
    }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletHolder {
    return WalletHolder(
      LayoutInflater.from(parent.context).inflate(
        R.layout.item_wallet,
        parent,
        false
      )
    )
  }

  override fun getItemCount(): Int {
    return wallets.size
  }

  override fun onBindViewHolder(holder: WalletHolder, position: Int) {
    holder.view.address.text = "Wallet Address: " + wallets[position].address
    holder.view.balance.text = "Balance: " + wallets[position].balance + " ETH"
    //holder.view.privateKey.text = "Private Key: " + wallets[position].privateKey
  }

  class WalletHolder(val view: View) : RecyclerView.ViewHolder(view)
}
