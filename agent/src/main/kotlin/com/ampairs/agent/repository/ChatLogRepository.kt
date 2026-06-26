package com.ampairs.agent.repository

import com.ampairs.agent.domain.model.ChatLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatLogRepository : JpaRepository<ChatLog, Long>
