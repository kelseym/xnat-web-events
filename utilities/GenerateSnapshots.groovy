import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import org.nrg.xdat.XDAT
import org.nrg.xdat.om.XnatExperimentdata
import org.nrg.xdat.services.AliasTokenService

def f = new File('/tmp/' + scriptId + '.out')
f.write("\nuser=" + user.getID())
f.append("\nscriptId=" + scriptId.toString())
if (this.hasProperty('event') && event) {
    f.append("\nevent=" + event.toString())
}
if (this.hasProperty('srcEventId') && srcEventId) {
    f.append("\nsrcEventId=" + srcEventId.toString())
}
if (this.hasProperty('srcEventClass') && srcEventClass) {
    f.append("\nsrcEventClass=" + srcEventClass.toString())
}
if (this.hasProperty('srcWorkflowId') && srcWorkflowId) {
    f.append("\nsrcWorkflowId=" + srcWorkflowId.toString())
}
if (this.hasProperty('scriptWorkflowId') && scriptWorkflowId) {
    f.append("\nscriptWorkflowId=" + scriptWorkflowId.toString())
}
f.append("\ndataType=" + dataType.toString())
f.append("\ndataId=" + dataId.toString())
f.append("\nexternalId=" + externalId.toString())
if (this.hasProperty('workflow') && workflow) {
    f.append("\nworkflow=" + workflow.toString())
}
f.append("\narguments = ")
f.append(arguments)

def localhost = null
try {
    localhost = InetAddress.getLocalHost()
} catch (UnknownHostException unknownHostException) { 
    println  "ERROR:  Unknown Host (" + unknownHostException.getClass().toString() + "): " +
        unknownHostException.getMessage()
    throw unknownHostException
}

// def slurper = new groovy.json.JsonSlurper()
// def args = slurper.parseText(arguments)

def experimentData = XnatExperimentdata.getXnatExperimentdatasById(dataId, user, false)
def sessionLabel = experimentData.getLabel()
def projectId = externalId.toString()

def token = XDAT.getContextService().getBean(AliasTokenService.class).issueTokenForUser(user)
def tempDir = Files.createTempDir()


private def executeOnShell(String command, File workingDir) {
  println command
  def process = new ProcessBuilder(addShellPrefix(command))
                                    .directory(workingDir)
                                    .redirectErrorStream(true) 
                                    .start()
  process.inputStream.eachLine { println it }
  process.waitFor();
  return process.exitValue()
}
 
private def addShellPrefix(String command) {
  commandArray = new String[3]
  commandArray[0] = "sh"
  commandArray[1] = "-c"
  commandArray[2] = command
  return commandArray
}

try {
    def command = "/data/intradb/pipeline/image-tools/WebBasedQCImageCreator" +
        " -host https://" + localhost.getCanonicalHostName() + 
        " -project " + projectId + 
        " -session " + sessionLabel + 
        " -xnatId " + dataId + 
        " -u " + token.getAlias() + 
        " -pwd " + token.getSecret() + 
        " -raw"
    executeOnShell(command, tempDir)
} catch (e) {
    println "EXCEPTION:  " + e
}

FileUtils.deleteDirectory(tempDir);
