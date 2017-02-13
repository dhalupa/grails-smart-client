package org.grails.plugins.smartclient

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.config.Scope

import java.util.concurrent.ConcurrentHashMap

/**
 * @author Denis Halupa
 */
@Slf4j
class ConversationScope implements Scope {

    static ThreadLocal<String> CURRENT_CONVERSATION = new ThreadLocal<>()
    private Map<String, ConversationHolder> map = new ConcurrentHashMap()


    @Override
    Object get(String name, ObjectFactory<?> objectFactory) {
        String conversationId = CURRENT_CONVERSATION.get()
        log.debug('Fetching from conversation for {}', conversationId)
        if (!map[conversationId]) {
            map[conversationId] = new ConversationHolder(timestamp: System.currentTimeMillis(), content: objectFactory.object)
        }
        def holder = map[conversationId]
        holder.timestamp = System.currentTimeMillis()
        return holder.content
    }

    @Override
    Object remove(String name) {
        return null
    }

    void removeExpired() {
        long current = System.currentTimeMillis()
        int i = 0
        map.keySet().each {
            if ((current - map[it].timestamp) > 1000 * 60 * 60) {
                map.remove(it)
                i++
            }
        }
        log.info('Removed {} expired workspaces from conversation', i)
    }

    @Override
    void registerDestructionCallback(String name, Runnable callback) {

    }

    @Override
    Object resolveContextualObject(String key) {
        return null
    }

    @Override
    String getConversationId() {
        return null
    }

    static class ConversationHolder {
        long timestamp
        def content

    }
}
