Name: Max Hayne
Course: CS455
Assignment 1

Running 'gradle build' from this folder should build my project. My Registry
and MessagingNode programs take arguments as specified in the assignment
description, so the scripts you're using to start up the Registry and 
MessagingNodes should work without any sort of custom configuration. The
'start-nodes.sh' script works when it is placed in the same place that this 
README is located.

As a side note, the Registry waits a constant time of about 15 (or 20?) seconds
between when it receives confirmation that all nodes have finished sending 
(not relaying) messages. I've set this specific amout of time for convenience,
as it will take longer than 15 seconds to finish relaying messages in a
worst-case-scenario, like when using a network of 128 nodes and 1 routing
entry per routing table. On the other hand, 15 seconds should be more than
enough time for the majority of stress tests.

I get perfect matches between sending and receiving in the majority of the
tests that I run, but sometimes I receive fewer packets than I send, and
therefore the sums are off as well. I'd love to get some feedback on what
I can do better to ensure a 100% packet delivery, as this has been a problem
that I've been trying to deal with for some time, and I'm just not quite sure
where, or if, I'm going wrong.
