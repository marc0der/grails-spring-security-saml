includeTargets << grailsScript('_GrailsBootstrap')

printMessage = { String message -> event('StatusUpdate', [message]) }
