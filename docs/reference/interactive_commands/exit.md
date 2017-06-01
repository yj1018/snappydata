#exit
Completes the `snappy` application and halts processing.

##Syntax

``` pre
EXIT
```

<a id="rtoolsijcomref33358__section_1AB9A85434CD41D69CB1F13ABCF0AE90"></a>
##Description

Causes the `snappy` application to complete and processing to halt. Issuing this command from within a file started with the [Run](../../reference/command_line_utilities/store-run/) command or on the command line causes the outermost input loop to halt.

`snappy` exits when the Exit command is entered or if given a command file on the Java invocation line, when the end of the command file is reached.

##Example


``` pre
snappy(localhost:<port number>)> disconnect peerclient;
snappy> exit;
```

