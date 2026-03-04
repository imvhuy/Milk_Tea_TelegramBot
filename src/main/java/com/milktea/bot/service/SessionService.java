package com.milktea.bot.service;

import com.milktea.bot.model.UserSession;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private final ConcurrentHashMap<Long, UserSession> sessions = new ConcurrentHashMap<>();

    public UserSession getSession(long chatId) {
        return sessions.computeIfAbsent(chatId, UserSession::new);
    }

    public void removeSession(long chatId) {
        sessions.remove(chatId);
    }
}
