ssh.run {
    session(ssh.remotes.web01) {
        result = execute 'uname'
    }
}