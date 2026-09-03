# bootclient by prbcdev

![Application](https://github.com/ATLauncher/ATLauncher/workflows/Application/badge.svg?branch=master)

## what is the purpose of bootclient

bootclient is an unofficial fork of the ATLauncher project for Minecraft which aims to rework the client with a 
minimalist 
approach

## changes for bootclient(last updated 03/09/26)

- multiple former tabs have been merged and redundant unused tabs have been cleanly removed
- new tabs are "play", "home", "modpacks" & "settings"
- the play tab combines the old "instances" tab and the "create instances"; they have now been combined into two 
  separate panels that allow for creation and play on the same tab
- the new "home" tab is the default landing page for when the launcher is first opened—it moved the old account 
  login to its own distinct panel with the about section, news and miscellaneous information contained in the other 
  panel on this tab
- the old "packs" tab has been entirely reworked from scratch into a singular standardized modpack browser; it merges 
  all the different source websites 
  into one universal search browser featuring the modpacks, displaying their source with an icon accompanied by 
  the modpack title and an install button alongside a link to 
  the corresponding official website
- other minor changes such as removing visual clutter and other unused options

## to do list for bootclient

- custom theme
- remove all name references to ATLauncher and change them to fit the current project name

## links to source ATLauncher project(not affiliated)

[ATLauncher Website](https://atlauncher.com) │ [ATLauncher Discord](https://atl.pw/discord) │
[ATLauncher Twitter](https://twitter.com/ATLauncher)


## prerequisites 

java 8 or above + gradle(run with `./gradlew`)

## building

build this project by running:

```sh
./gradlew build
```

this will build the application and output the resulting files for Windows in the `dist` directory

## running in test

if you want to run the launcher while developing with it, you can use your IDE to do so

alternatively run:

```sh
./gradlew run --args="--debug --working-dir=testLauncher"
```

please do not remove the `--working-dir=testLauncher` argument when testing

## using an IDE

the recommended IDEs are either [IntelliJ IDEA](https://www.jetbrains.com/idea/) 
or [VSCode](https://code.visualstudio.com/) for this project

## checking dependency updates

To check for dependency updates with gradle, simply run:

```sh
./gradlew dependencyUpdates
```

This will print a report to the console about any dependencies which have updates

## bootclient custom theme

while the original ATLauncher application supports custom themes, the bootclient fork is limited to a default dark and 
light 
mode only

## licensing as per the original ATLauncher master branch

"this work is licensed under the GNU "General Public License" v3.0. to view a copy of this license, visit
<http://www.gnu.org/licenses/gpl-3.0.txt>.

a simple way to keep in terms of the license is by forking this repository and leaving it open source under the same
license. we love free software, seeing people use our code and then not share the code, breaking the license, is
saddening. so please take a look at the license and respect what we're doing.

also, while we cannot enforce this under the license, you cannot use our CDN/files/assets/modpacks on your own launcher.
again we cannot enforce this under the license, but needless to say, we'd be very unhappy if you did that and really
would like to leave cease and desist letters as a last resort."
