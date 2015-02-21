# TXTR

TXTR: Light accounts and messaging made simple

![screenshots](https://raw.githubusercontent.com/juanignaciomolina/txtr/master/art/Composed_image.png)

## Design principles and service characteristics

* Provide super fast XMPP accounts creation
* Be as beautiful and easy to use as possible without sacrificing security or
  privacy
* Rely on existing, well established protocols (XMPP)
* Do not require a Google Account or specifically Google Cloud Messaging (GCM)
* Require as few permissions as possible
* Keep users info requirements as low as possible

## Features

* End-to-end encryption with either [OTR](https://otr.cypherpunks.ca/) or [OpenPGP](http://www.openpgp.org/about_openpgp/)
* Sending and receiving images and files
* Indication when your contact has read your message
* Intuitive UI that follows Android Design guidelines
* Pictures / Avatars for your Contacts
* Syncs with desktop client and other mobile sessions
* Conferences / Group chat
* Address book integration
* Multiple accounts / unified inbox
* Very low impact on battery life


### XMPP Features

TXTR is a fork of the Conversations project (see below), therefore it 
works with every XMPP server out there. However XMPP is an
extensible protocol. These extensions are standardized as well in so called
XEP's. Conversations supports a couple of these to make the overall user
experience better. There is a chance that your current XMPP server does not
support these extensions; therefore to get the most out of TXTR you
should consider either switching to an XMPP server that does or — even better —
run your own XMPP server for you and your friends. These XEP's are:

* XEP-0065: SOCKS5 Bytestreams (or mod_proxy65). Will be used to transfer
  files if both parties are behind a firewall (NAT).
* XEP-0163: Personal Eventing Protocol for avatars
* XEP-0198: Stream Management allows XMPP to survive small network outages and
  changes of the underlying TCP connection.
* XEP-0280: Message Carbons which automatically syncs the messages you send to
  your desktop client and thus allows you to switch seamlessly from your mobile
  client to your desktop client and back within one conversation.
* XEP-0237: Roster Versioning mainly to save bandwidth on poor mobile connections
* XEP-0313: Message Archive Management synchronize message history with the
  server. Catch up with messages that were sent while Conversations was
  offline.
* XEP-0352: Client State Indication lets the server know whether or not
  Conversations is in the background. Allows the server to save bandwidth by
  withholding unimportant packages.
* XEP-0191: Blocking command lets you blacklist spammers or block contacts
  without removing them from your roster.

### Base code

TXTR uses the Conversations project by Daniel Gultsch as a base code

* [Original Repository](https://github.com/siacs/Conversations)

## FAQ

### General

#### How do I install TXTR?

TXTR is still under heavy development and it's not available in any app store right now. However,
you can import this project in Android Studio and compile it by yourself. Gradle is requiered though.

#### How do I create an account?

Press the "+" button on the Action Bar, wait for the server to provide you with an available PIN 
(it should take less than 2 seconds over Wi-Fi) and then press continue. You will be prompted to
upload an optional profile picture.

#### Where can I set up a custom hostname / port
Take a look inside the Config class in the project, you can modify the behavior of many features that way,
including the server associate domain name.

#### TXTR doesn't work for me and/or I found a bug. Where can I get help?


Please report any issue to our [issue tracker][issues]. If your app crashes please
provide a stack trace. If you are experiencing misbehaviour please provide
detailed steps to reproduce. Always mention whether you are running the latest
Play Store version or the current HEAD. If you are having problems connecting to
your XMPP server your file transfer doesn’t work as expected please always
include a logcat debug output with your issue (see above).

[issues]: https://github.com/juanignaciomolina/txtr/issues

#### I get 'delivery failed' on my messages

If you get delivery failed on images it's probably because the recipient lost
network connectivity during reception. In that case you can try it again at a
later time.

For text messages the answer to your question is a little bit more complex.
When you see 'delivery failed' on text messages, it is always something that is
being reported by the server. The most common reason for this is that the
recipient failed to resume a connection. When a client loses connectivity for a
short time the client usually has a five minute window to pick up that
connection again. When the client fails to do so because the network
connectivity is out for longer than that all messages sent to that client will
be returned to the sender resulting in a delivery failed.

Other less common reasons are that the message you sent didn't meet some
criteria enforced by the server (too large, too many). Another reason could be
that the recipient is offline and the server doesn't provide offline storage.

Usually you are able to distinguish between these two groups in the fact that
the first one happens always after some time and the second one happens almost
instantly.

#### Where can I see the status of my contacts? How can I set a status or priority?

Statuses are a horrible metric. Setting them manually to a proper value rarely
works because users are either lazy or just forget about them. Setting them
automatically does not provide quality results either. Keyboard or mouse
activity as indicator for example fails when the user is just looking at
something (reading an article, watching a movie). Furthermore automatic setting
of status always implies an impact on your privacy (are you sure you want
everybody in your contact list to know that you have been using your computer at
4am‽).

In the past status has been used to judge the likelihood of whether or not your
messages are being read. This is no longer necessary. With Chat Markers
(XEP-0333, supported by Conversations since 0.4) we have the ability to **know**
whether or not your messages are being read.  Similar things can be said for
priorities. In the past priorities have been used (by servers, not by clients!)
to route your messages to one specific client. With carbon messages (XEP-0280,
supported by Conversations since 0.1) this is no longer necessary. Using
priorities to route OTR messages isn't practical either because they are not
changeable on the fly. Metrics like last active client (the client which sent
the last message) are much better.

Making these status and priority optional isn't a solution either because
TXTR is trying to get rid of old behaviours and set an example for
other messaging services.

#### Conversations is missing a certain feature

Community ideas are much aprecciated in the development of TXTR. 
You can use the [issue tracker][issues] on GitHub to let us know of any improvement you can imagine. 
Please take some time to browse through the issues to see if someone else already suggested it. 

### Security

#### Why are there two end-to-end encryption methods and which one should I choose?

In most cases OTR should be the encryption method of choice. It works out of the
box with most contacts as long as they are online. However PGP can, in some
cases, (message carbons to multiple clients) be more flexible.

#### How do I use OpenPGP

Before you continue reading you should note that the OpenPGP support in
TXTR is experimental. This is not because it will make the app unstable
but because the fundamental concepts of PGP aren't ready for widespread use.
The way PGP works is that you trust Key IDs instead of JID's or email addresses.
So in theory your contact list should consist of Public-Key-IDs instead of
JID's. But of course no email or XMPP client out there implements these
concepts. Plus PGP in the context of instant messaging has a couple of
downsides: It is vulnerable to replay attacks, it is rather verbose, and
decrypting and encrypting takes longer than OTR. It is however asynchronous and
works well with message carbons.

To use OpenPGP you have to install the open source app
[OpenKeychain](www.openkeychain.org) and then long press on the account in
manage accounts and choose renew PGP announcement from the contextual menu.

#### How does the encryption for conferences work?

For conferences the only supported encryption method is OpenPGP (OTR does not
work with multiple participants). Every participant has to announce their
OpenPGP key (see answer above). If you would like to send encrypted messages to
a group chat you have to make sure that you have every participant's public key
in your OpenKeychain. Right now there is no check in TXTR to ensure that. 
You have to take care of that yourself. Go to the conference details and
touch every key id (The hexadecimal number below a contact). This will send you
to OpenKeychain which will assist you on adding the key.  This works best in
very small conferences with contacts you are already using OpenPGP with. This
feature is regarded experimental. The base project Conversations is the only client that uses
XEP-0027 with conferences. (The XEP neither specifically allows nor disallows
this.)

### Development

#### How do I build TXTR without and IDE such as Android Studio or Eclipse with ADT?

Make sure to have ANDROID_HOME point to your Android SDK

    git clone https://github.com/juanignaciomolina/txtr.git
    cd txtr
    ./gradlew build


[![Build Status](https://travis-ci.org/siacs/Conversations.svg?branch=development)](https://travis-ci.org/siacs/Conversations)

#### How do I debug TXTR

If something goes wrong TXTR usually exposes very little information in
the UI (other than the fact that something didn't work). However with adb
(android debug bridge) you squeeze some more information out of Conversations (and TXTR of course).
These information are especially useful if you are experiencing trouble with
your connection or with file transfer.

    adb -d logcat -v time -s conversations
