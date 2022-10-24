# Runner

## Github runner setup

### Reference

See

- [github instructions](https://github.com/organizations/firepandalabs/settings/actions/runners/new?arch=x64&os=linux)
- [youtube video](https://www.youtube.com/watch?v=G6nBM3NxBDc)

on how to setup the runner.

### Setup steps

ssh to the `runner` box and set it up there.

```shell
sudo apt update
sudo apt install build-essential git ranger docker.io
sudo usermod -aG docker $USER

# install az
# curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

# docker login with
<Space>docker login firepandalabs.azurecr.io --username <runner-application-id> --password <secret>
```

Setup the runner as a background service:

Go to this page first: [docs](https://github.com/organizations/firepandalabs/settings/actions/runners/new?arch=x64&os=linux)

```shell
mkdir -p ~/github-actions/runner

# this outputs the help 
sudo ./svc.sh

# install as a service
sudo ./svc.sh install

# start now
sudo ./svc.sh start

# stop
# sudo ./svc.sh stop

# check status
sudo ./svc.sh status

# NB we can also use journalctl to tail the log
sudo journalctl -u actions.runner<Press Tab> --follow

```
