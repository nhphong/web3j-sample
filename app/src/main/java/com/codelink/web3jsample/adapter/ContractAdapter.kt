package com.codelink.web3jsample.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.codelink.web3jsample.R
import com.codelink.web3jsample.data.Contract
import kotlinx.android.synthetic.main.item_contract.view.*

class ContractAdapter : RecyclerView.Adapter<ContractAdapter.ContractHolder>() {

  var contracts = listOf<Contract>()
    set(value) {
      field = value
      notifyDataSetChanged()
    }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractHolder {
    return ContractHolder(
      LayoutInflater.from(parent.context).inflate(
        R.layout.item_contract,
        parent,
        false
      )
    )
  }

  override fun getItemCount(): Int {
    return contracts.size
  }

  override fun onBindViewHolder(holder: ContractHolder, position: Int) {
    holder.view.address.text = "Contract Address: " + contracts[position].address
    holder.view.balance.text = "Balance: " + contracts[position].balance + " ETH"
    holder.view.owner.text = "Owner: " + contracts[position].owner
  }

  class ContractHolder(val view: View) : RecyclerView.ViewHolder(view)
}
