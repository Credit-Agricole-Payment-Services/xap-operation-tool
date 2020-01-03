# xap-operation-tool

This is the source code for ```xap-operation-tool```.

```xap-operation-tool``` eases usage and administration of XAP, the In-Memory Computing product of GigaSpaces.

## Features

The project aims to deliver a tool that aims to ease operational tasks on a XAP instance.

The tool is written in Java, and needs a Java Virtual Machine.

Binaries are usable with a CLI (command line interface). 

Main features includes : 
- deploying Processing Units
- un-deploying Processing Units
- restarting containers
- restarting managers
- shutting down agents
- demoting of primary instances
- rebalancing Processing Units instances across available containers/machines/zones
- trigger thread dump
- trigger heap dump
- trigger Garbage Collector
- kill agents by ID
- start a new container based on existing XML configuration

Main benefit is that one command line allows to perform the task on either just one specific component, or all of them.

On rather large systems with many instances of components, this allows to perform maintenance tasks easily and consistently.

## License

This project is licensed under ```Apache License 2.0```.

Copyright (c) Credit Agriole Payment Services since 2018.

Initial Authors and Contributors are : Jean-Yves CRONIER, Yann ROBERT, and Bastien JANSEN.

Credit Agriole Payment Services is the Licensor, releasing the project under Apache License 2.0. 

*Disclaimer of Warranty*

Unless required by applicable law or
agreed to in writing, Licensor provides the Work (and each
Contributor provides its Contributions) on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied, including, without limitation, any warranties or conditions
of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
PARTICULAR PURPOSE. You are solely responsible for determining the
appropriateness of using or redistributing the Work and assume any
risks associated with Your exercise of permissions under this License.

See LICENSE for more details.

## Acknowledgements

```XAP``` is a product of GigaSpaces.

```XAP``` and ```GigaSpaces``` are trademarks of GigaSpaces. Any references to this terms are for fair use only.
Any references to terms and vocabulary related to GigaSpaces technology are for fair use only.

The project ```xap-operation-tool``` is not affiliated with GigaSpaces.

Project authors and contributors are not making any claim on any property of GigaSpaces.
