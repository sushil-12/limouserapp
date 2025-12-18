package com.example.limouserapp.data.local

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared data store for passing data between screens
 * This is a simple in-memory store for temporary data
 */
@Singleton
class SharedDataStore @Inject constructor() {
    
    private var _tempUserId: String? = null
    private var _phoneNumber: String? = null
    private var _accountId: Int? = null
    
    fun setTempUserId(tempUserId: String) {
        _tempUserId = tempUserId
    }
    
    fun getTempUserId(): String? = _tempUserId
    
    fun setPhoneNumber(phoneNumber: String) {
        _phoneNumber = phoneNumber
    }
    
    fun getPhoneNumber(): String? = _phoneNumber
    
    fun setAccountId(accountId: Int) {
        _accountId = accountId
    }
    
    fun getAccountId(): Int? = _accountId
    
    fun clearData() {
        _tempUserId = null
        _phoneNumber = null
        _accountId = null
    }
}
