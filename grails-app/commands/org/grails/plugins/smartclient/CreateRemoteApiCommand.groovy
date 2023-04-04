package org.grails.plugins.smartclient


import grails.dev.commands.GrailsApplicationCommand
import groovy.transform.CompileStatic
import org.grails.io.support.ClassPathResource
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
class CreateRemoteApiCommand implements GrailsApplicationCommand {
    @Autowired
    RemoteApiJavaScriptProvider javaScriptProvider
    @Autowired
    ConfigProvider configProvider


    @Override
    boolean handle() {
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
