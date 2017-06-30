# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure("2") do |config|
  config.vm.box = "centos/7"
  config.vm.box_check_update = false

  # Create a forwarded port mapping which allows access to a specific port
  # within the machine from a port on the host machine. In the example below,
  # accessing "localhost:8080" will access port 80 on the guest machine.
  # config.vm.network "forwarded_port", guest: 80, host: 8080

  # Create a private network, which allows host-only access to the machine
  # using a specific IP.
  # config.vm.network "private_network", ip: "192.168.33.10"

  config.vm.provision "shell", inline: <<-SHELL
    yum install -y git java-1.8.0* wget

    mkdir -p /opt/apache
    cd /opt/apache/

    wget ftp://mirror.reverse.net/pub/apache/maven/maven-3/3.5.0/binaries/apache-maven-3.5.0-bin.tar.gz
    wget http://mirror.stjschools.org/public/apache//db/derby/db-derby-10.13.1.1/db-derby-10.13.1.1-bin.tar.gz

    tar -xzvf apache-maven-3.5.0-bin.tar.gz
    tar -xzvf db-derby-10.13.1.1-bin.tar.gz

    # git clone https://github.com/doecode/server  # Not needed due to rsyncing into /vagrant

    echo "export DERBY_INSTALL=/opt/apache/db-derby-10.13.1.1-bin" >> ~/.bashrc

    nohup /opt/apache/db-derby-10.13.1.1-bin/bin/startNetworkServer &

    mkdir -p ~vagrant/.m2
    cp /vagrant/derby_sample.xml ~vagrant/.m2/settings.xml

    chown -Rc vagrant:vagrant ~vagrant/

    su - vagrant nohup bash -c "cd /vagrant && /opt/apache/apache-maven-3.5.0/bin/mvn -P doecode jetty:run > /tmp/server.log 2>&1" &
  SHELL
end
