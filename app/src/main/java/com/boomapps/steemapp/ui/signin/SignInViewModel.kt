/*
 * Copyright 2018, BoomApps LLC. 
 * All rights reserved.
*/
package com.boomapps.steemapp.ui.signin

import android.util.Log
import com.boomapps.steemapp.SteemApplication
import com.boomapps.steemapp.repository.RepositoryProvider
import com.boomapps.steemapp.repository.UserData
import com.boomapps.steemapp.repository.network.NetworkRepository
import com.boomapps.steemapp.repository.network.NetworkResponseCode
import com.boomapps.steemapp.repository.steem.SteemErrorCodes
import com.boomapps.steemapp.ui.BaseViewModel
import com.boomapps.steemapp.ui.ViewState
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


/**
 * Created by Vitali Grechikha on 24.01.2018.
 */
class SignInViewModel : BaseViewModel() {

    var nickname: String = ""
    var postingKey: String = ""
    var activeKey: String = ""
    var loginResult = -1

    companion object {
        val LOG_TAG = SignInViewModel::class.java.simpleName
        val LOGIN_SUCCESS = 1
        val LOGIN_ERROR_BAD_DATA = 2
        val LOGIN_ERROR_NETWORK_ISSUE = 3
    }

    init {
        state.value = ViewState.COMMON
    }


    fun login(): Boolean {
        if (nickname.isNotEmpty()) {
            SteemApplication.instance.saveUserName(nickname.toLowerCase())
        } else {
            return false
        }
        state.value = ViewState.PROGRESS
        Flowable.fromCallable {
            return@fromCallable RepositoryProvider.getSteemRepository().login(nickname.toLowerCase(), postingKey)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    if (!it.result) {
                        if (it.errorMessage != null) {
                            stringError = it.errorMessage
                        }
                        when (it.errorCode) {
                            SteemErrorCodes. INCORRECT_USER_DATA_ERROR -> {
                                loginResult = LOGIN_ERROR_BAD_DATA
                            }
                            else -> {
                                loginResult = LOGIN_ERROR_NETWORK_ISSUE
                            }
                        }
                    } else {
                        loginResult = LOGIN_SUCCESS
                    }
                    Log.d("SignInViewModel", "doOnNext")
                }
                .doOnComplete {
                    Log.d("SignInViewModel", "doOnComplete")
                    if (loginResult == LOGIN_SUCCESS) {
                        RepositoryProvider.getPreferencesRepository().saveUserData(UserData(nickname, null, null, postingKey))
                        loadFullAdditionalData(nickname)
                    } else {
                        state.value = ViewState.FAULT_RESULT
                    }
                }
                .doOnError {
                    Log.d("SignInViewModel", "doOnError")
                    stringError = it.localizedMessage
                    state.value = ViewState.FAULT_RESULT
                }
                .subscribe()

        return true
    }

    private fun loadFullAdditionalData(nick: String) {
        RepositoryProvider.getNetworkRepository().loadFullStartData(nick, object : NetworkRepository.OnRequestFinishCallback<Any?> {

            override fun onSuccessRequestFinish(response: Any?) {
                state.value = ViewState.SUCCESS_RESULT
            }

            override fun onFailureRequestFinish(code : NetworkResponseCode, throwable: Throwable) {
                stringError = if (throwable.localizedMessage == null) {
                    throwable.message ?: ""
                } else {
                    throwable.localizedMessage
                }
                state.value = ViewState.FAULT_RESULT
            }
        })
    }
}