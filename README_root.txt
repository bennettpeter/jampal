Ubuntu or Debian users can add my repository to their system.

Run the following commands
sudo add-apt-repository ppa:pgbennett/ppa
sudo apt-get update

Alternatively, add the following lines to your /etc/apt/sources.list
deb http://ppa.launchpad.net/pgbennett/ppa/ubuntu precise main
deb-src http://ppa.launchpad.net/pgbennett/ppa/ubuntu precise main

Now you can install the latest version of jampal using Ubuntu software center,
synaptic or apt-get.

