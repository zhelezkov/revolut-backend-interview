package com.rsredsq.revolut.backend.domain.repository

import com.rsredsq.revolut.backend.domain.Account

interface AccountRepository {
  fun create(balance: Double = 0.0): Account
  fun findAll(): List<Account>
  fun findById(id: Int): Account?
  fun delete(id: Int)
}