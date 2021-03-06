package org.grails.plugins.smartclient

import grails.dev.commands.ApplicationCommand
import grails.dev.commands.ExecutionContext
import groovy.transform.CompileStatic
import org.grails.io.support.ClassPathResource
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
class CreateRemoteApiCommand implements ApplicationCommand {
    @Autowired
    RemoteApiJavaScriptProvider javaScriptProvider
    @Autowired
    ConfigProvider configProvider

    boolean handle(ExecutionContext ctx) {
        File f = new File(configProvider.remoteApiFileName)
        f.delete()
        f.write(javaScriptProvider.createRemoteAPI())

        f = new File(configProvider.smartMvcFileName)
        f.delete()
        ClassPathResource resource = new ClassPathResource('js/SmartMvc.js')
        f.write(resource.inputStream.getText('UTF-8'))

        return true
    }
}
