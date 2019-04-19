package com.codelink.web3jsample.db

import android.content.Context
import android.preference.PreferenceManager
import com.codelink.web3jsample.data.Contract
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class Caching(private val context: Context) {

  private val pref by lazy {
    PreferenceManager.getDefaultSharedPreferences(context)
  }

  private val gson by lazy {
    Gson()
  }

  fun loadContracts(): List<Contract> {
    val inputJson = pref.getString(CONTRACTS_KEY, null)
    val contractListType = object : TypeToken<List<Contract>>() {}.type
    var result = emptyList<Contract>()
    try {
      result = gson.fromJson<List<Contract>>(
        inputJson, contractListType
      ) ?: emptyList()
    } catch (e: Exception) {
      e.printStackTrace()
    }

    return result
  }

  fun saveContract(contract: Contract) {
    val contracts = loadContracts().toMutableList().also {
      it.add(contract)
    }
    val json = gson.toJson(contracts)
    pref.edit()
      .putString(CONTRACTS_KEY, json)
      .apply()
  }

  fun clearContracts() {
    pref.edit()
      .putString(CONTRACTS_KEY, "")
      .apply()
  }

  private companion object {
    const val CONTRACTS_KEY = "contracts"
  }
}
