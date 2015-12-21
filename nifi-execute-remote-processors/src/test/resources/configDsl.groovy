remotes {
    web01 {
        role 'masterNode'
        host = 'localhost'
        user = 'sumo'
        password = 'demo'
        knownHosts = allowAnyHosts
    }
    web02 {
        host = '192.168.1.5'
        user = 'sumo'
        knownHosts = allowAnyHosts
    }
}