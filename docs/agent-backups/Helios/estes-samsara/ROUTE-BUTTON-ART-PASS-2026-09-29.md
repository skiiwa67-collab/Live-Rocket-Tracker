# Estes driver HUD — route button art pass

Date: 2026-09-29, about 10:15 AM CT.
Owner: Da Vinci. Estes tablet, not Live Rocket Tracker. No Play.


## Scope correction, 10:28 AM CT

Chris is looking at the first board and narrowing it. Do not redesign the tablet. The only layout change is the swap: Routes takes the size and place of the current Tasks button, and Tasks takes the size and place of the current Routes button. Forms stays. Everything else on the screen is shown as it is today.

Redraw the board on the real Samsara home, the September 19 6:42 AM screenshot at samsara-home-2026-09-19-0642.jpg. Olsen, truck 61784, Driving, the hours-of-service rings, Coach 96, and the other tiles stay where they are.

Hours of service stays on screen at all times. It is a safety requirement. Do not remove it. Do not move it.

Coach is also a safety element. Do not change it in this pass. A size change might be possible later. Not now.

The six color chips beside Sign out are not in this redraw. They would change the screen around the swap, and Chris just said that screen stays as it is.

The content inside the big Routes button still stands: the September 19 numbers, the three colors, the fuel, shop, and wash icons, and the closest company fuel stop when fuel is crossed out.


## What Chris wants

The route button becomes the large one. It should be as big as the current tasks button. Tasks shrinks to about the size of the forms button. Forms stays where it is and stays that size, because a breakdown means opening forms and filling them out.

Chris's words: tasks should be as small as forms. Forms stays. Tasks needs to be as small as routes, and routes needs to be as big as the task button. Read that as: routes grows to the current task-button size, tasks shrinks to the forms size. Do not shrink routes.

## What the big route button shows

Everything has to fit inside that button. Use the real screenshot numbers. Do not invent the trailer.

- Trailer: 516335 (six digits, from the September 19 route departure form)
- Second trailer field on that form was MTY, not a second number
- Pup numbers start with 2. Examples he gave: 2213, 2576. Those can be invented.
- Dolly number, or the label "53-foot van" when he is pulling a van instead of a set
- Origin or current location
- In-between stops
- Destination (the form showed destination terminal 089)
- Weight from the Samsara dashboard only if it fits

Also draw the tasks button at the new smaller size, next to forms, so the scale is obvious.


## Three color states

Show the big route button in all three colors, with the same trailer, pup, dolly or van, and weight content. Added 10:17 AM CT.

- Green: the route has proper authorization. Everything is good to go.
- Yellow: the trailer or the tractor is sitting at the wrong location. He can still move it. The dispatcher needs a pingback so they can relocate the equipment, dispatch can happen properly, and the button can turn green.
- Red: absolute no-go. Wrong trailer, or the equipment has been flagged at the shop as not pullable, or a similar hard block.

Chris's words: red is an absolute no-go, you got the wrong trailer, something is wrong with the equipment, it has been flagged at the shop that that trailer can't be pulled. Yellow is the trailer or the tractor sitting at the wrong location. You can still move it, but the dispatcher needs to be pinged so they move the equipment to the proper location, dispatch can happen properly, and the button can turn green.

## References already on disk

- Home screen: /workspace/agent-backups/helios/estes-samsara/samsara-home-2026-09-19-0642.jpg
  Olsen, truck 61784, Driving, HoS rings, Coach 96, route LH35082845 completed, Forms tile. This is the current layout. Routes is the small tile. Tasks is the larger empty tile.
- Route form: /workspace/agent-backups/helios/estes-samsara/route-departure-van-mty-2026-09-19.jpg
  Route Departure Form 2, 7:07 AM Sep 19. Driver 435769. Dispatch LH35083527. Trailer 516335. Destination 089. Trailer 2 field /MTY.

Check these before inventing the layout.

## Color chips next to sign out

Chris, voice, 2026-09-29 about 10:22 AM CT. He remembers this from an earlier talk. It was not in the September 19 spec.

Da Vinci's art style adds a color picker in the blank space next to Sign out. About six chips. The driver picks the one he likes.

- Bright white. The current glare. Almost nobody picks it, unless they are in hard sunlight.
- Da Vinci parchment. His manuscript colors.
- High contrast.
- Completely black. The dark one.
- A gray, in between.
- One pretty stylized option.

No art for these chips has been found. This is the ask.

## Terminal service icons

From the Saturday, September 19, 2026 call at 10:06 AM CT (call-314f78da-6b53-4f69-8c79-5f0a57df4fed, transcript 08-1006-sincera-dod-routes-fuel-icons). Chris brought it back on the September 29 call as something the driver should see at a glance on or near the route button.

Draw fuel, shop, and wash-bay icons on the big route button, for the terminal he is heading to. A service the terminal does not have is the same icon crossed out. He specifically called the missing-fuel mark a red cross-out: we do not have fuel here.

When fuel is crossed out, the big route button itself shows the closest company fuel stop: brand or name, address, and distance. That is Chris on September 29. On September 19 he had left the same information as a tap, then a flyout or a new window, not painted in the button. Do not draw a map. He copies the address into his own nav app.

The longer list, if you show it at all, keeps the September 19 colors: green for the company preferred stop, a second stop for an emergency, and red only if he is running on fumes. Real-time "this station is out of diesel" was left as a sales question, not art.

## Drop the LH number, 10:29 AM CT

Chris, looking at the board. The LH dispatch number (drawn as LH35083527; he said it as LH 303583527) has never needed to be shown to the driver. Remove it.

That space shows origin and destination instead. Destination is terminal 089, from the September 19 form. Origin was not on that form. Do not invent a terminal code. Show both labels, and leave origin blank unless it is actually on the September 19 screenshot.

This does not change the scope correction. Swap only routes and tasks. Hours of service stays. Coach stays. Color chips stay off this redraw.

## Weight on the big button, 10:29 AM CT

Chris, same call. The big route button has the room, so show the weight on it if it fits. This is the September 19 note that weight comes in only when the button is the large size.

The September 19 home screen and the departure form do not show a weight. Do not invent pounds. Show the weight line, and leave the number blank.

## Origin and destination together, 10:30 AM CT

Chris clarified. Origin and destination are not a choice. Both are shown in the space where the LH number was. Not origin instead of destination, and not destination instead of origin.

Destination value is terminal 089. An origin code was not on the September 19 form. Do not invent one. Show the origin label and leave its value blank until he gives it.

## Drill-down, 10:30 AM CT

Chris described the current dig. It takes about five screens. He taps the route button and gets more info. He taps origin, which is a text field, and gets more info again. Only then can he enter the trailer, the dolly, and the other fields.

The top-level route button still shows origin and destination together. Once he opens that button, origin and destination become separate taps. Each tap goes straight to that entry, and skips the extra screens. The screen he is trying to reach is the one where he enters the trailer, the dolly, and the other fields.

This is a behavior note for the art. Do not draw the five-screen dig as the new design.

## Yellow label, 10:31 AM CT

On the yellow route button, the badge that says Wrong yard changes to Incorrect data. That is the label Chris wants on the yellow state. The yellow meaning stays: he can still move the equipment, and dispatch gets a ping so it can be fixed and the button can turn green.

## Full screen, hard, 10:32 AM CT

Chris is looking at the latest board. It still drops the rest of the screen. That is wrong. Redraw on the September 19 6:42 AM home photo, samsara-home-2026-09-19-0642.jpg. Copy that screen. The only change is that Routes and Tasks exchange size and place. Routes becomes the large left tile. Tasks becomes the smaller tile Routes occupies now. Forms stays.

These stay visible, in the same places:

- The status bar, the four-square app switcher, and the settings gear
- The blue circle, Olsen; A., September 19, and truck 61784
- Status, Vehicles, Team, and Sign out
- Hours of service: Break, Drive, Shift, Cycle, and the Driving badge. Do not remove them. Do not move them.
- Coach, the score 96, "Drive smart, stay safe!", and the 8x, Speed, HoS, and Eco marks. Do not change Coach this pass.
- Training, Documents, Trip Log, and Qualifications
- The bottom bar: home, messages, the center button, the person, and the fingerprint

Do not invent a simplified card. Do not drop a button to make room. The route-button content (origin, destination, trailer, weight line, three colors, fuel, shop, wash) goes inside the large Routes tile only.

## Six color versions, 10:35 AM CT

Chris likes the current full-screen layout. Do not move anything.

Put the six color chips back, to the right of Sign out. Bright white is the hard-sunlight glare, and almost nobody will want it. Then Da Vinci parchment, high contrast, full black, a gray in-between, and one pretty option.

Make six versions of this same screen, one per chip. Only the colors change. The layout stays. Each version shows that chip selected, and the screen and the route button use that palette.

The three status states still have to read apart: authorized, incorrect data, and do not pull. Do not let the theme erase those.

## Origin is filled, 10:35 AM CT

Chris sent three September 19 photos, saved in sep19-route-screens/. The origin is no longer blank.

Origin is terminal 193, 5200 East Loop 820 South, Fort Worth, Texas 76119. Depart 8:58 AM. The screen reads Originating Terminal TID-193/435769.

Destination stays terminal 089. The future-stops screen shows arrival 9:32 AM. The screen reads Destination Terminal TID-089/435769.

The departure form on the same photos still shows trailer 516335, trailer 2 field /MTY, dolly required if there are two trailers, and both trailer destinations 089. The LH number is on these screens. It still does not go on the route button.

## Trailer numbers and live terminals, 10:38 AM CT

A trailer number that starts with 5 is a van. Chris called it a 50-footer, and it is a 53-foot van. A number that starts with 2 is a pup, a 24-footer. His example of a set is 216335 and 239474. That pair is an example, not this load. This load stays trailer 516335, a van.

If he is pulling a set, both pup numbers show on the button. A dolly is required when there are two trailers. He scrolls down to enter the dolly number.

Origin and destination are live fields, not examples baked into the art. This load is origin 193 and destination 089. The next load may be different. The button shows whatever Samsara and the AS400 send.

Weight stays on the button. Chris likes the field. The number stays blank until the system has one.

## Pup set mock, 10:39 AM CT

Chris wants a second full-screen mock, a pup set, not only the van. Same Saturday screen. Do not replace the van board. The van stays trailer 516335.

Number rule, updated. A number that starts with 5 is a 53-foot van. A number that starts with 2 is a 24-foot pup. A number that starts with 9 is also a pup. Pups usually start with 9. His example pup is 933746. His example set is still 216335 and 239474.

Two states of the route button, both on the full screen:

- When dispatch sends the load, the button shows the two trailer numbers and a blank space for the pup.
- After he drills down and enters the pup, he goes back out and that pup number is on the route button. Use 933746 for the entered pup. The button shows what he entered.

A dolly is still required when there are two trailers. He scrolls down to enter it.

## Color theme box, 10:53 AM CT

This replaces the row of six chips running beside Sign out. Chris says six across takes too much space.

Put the six choices in their own box labeled Color theme. Use a 3 by 2 layout so it stays compact. The six options stay: White, Parchment, Contrast, Black, Gray, Styled.

Chris likes the navy and gold screen (Styled). White, Parchment, Contrast, and Black stay as they are. He is leaning toward Parchment for production. That is a lean, not a lock.

Redo only Gray. Do not lay a gray field over the whole screen. That overlay dimmed the other content and fuzzed it. Keep the gray palette, keep the layout frozen, and keep the status bar, hours of service, Coach, and the route button readable.


## Pup set weights, 10:54 AM CT

When he is pulling a set there are three weights. Each pup has its own weight, plus a total.

Order is pup, dolly, pup. That is how drivers build a set. Not pup, pup, dolly.

Each pup's weight field sits to the right of that pup. The total weight sits underneath, offset to the right, the way weight currently sits under the dolly on the van button.

Remove the extra pup line. Chris thinks that line was accidental.

The structure is:

- pup number, then that pup's weight
- dolly number
- pup number, then that pup's weight
- total weight, offset to the right

Weight numbers stay blank until the system has them. Do not invent pounds.

The Color theme box from 10:53 AM CT is still not on the gray file. Chips are still six across. That change is still open. Gray itself is accepted as the redo with no heavy overlay.


## Alpha code beside the terminal number, 10:55 AM CT

On the route button, show the alpha code next to each terminal number. Drivers should see both.

This load: origin 193 ARL, destination 089 DLT. Pair them that way, or the same pairing if the line breaks. Do not show the number alone.

193 ARL is Chris's word on this call. The sample file had no letter code on 193 until this note. 089 DLT matches the Dallas row already in that file.


## Color theme box, second pass, 10:57 AM CT

This replaces the earlier hold that left White, Parchment, Contrast, and Black with the old row of chips. All six screens get the Color theme box, 3 by 2, to the right of Sign out.

The pickers are too small. They do not fill the box, and drivers have big fingers. Make each picker large enough to tap easily. Fill the box.

Names:

- White stays White.
- Parchment becomes Da Vinci.
- Contrast stays Contrast.
- Black becomes Night.
- Gray stays Gray.
- The stylized option is no longer navy and gold. It is company-colored after the Estes logo: red, gold, and black.

Terminal numbers on every screen, van and pup: origin 193 ARL, destination 089 DLT. Do not show the number alone.


## Chip rename, 11:06 AM CT

Rename the sixth color theme option from Styled to Estes. The chip label and the screen name both say Estes. Do this on all six screens.

The screen itself stays. Gold field, red edges, black route card. The green authorized bar stays green. Do not repaint the palette.


## Full redraw of all six, 11:08 AM CT

Chris wants all six color screens redrawn as one matched set. A screen that looked fine on its own is not an exception. The Color theme box is on every one of them.

Box: 3 by 2, pickers large enough to tap and filling the box. Names: White, Da Vinci, Contrast, Night, Gray, Estes. Not Styled.

Gray stays the readable version. No heavy overlay.

Estes stays the current paint. Gold field, red edges, black route card. Green authorized bar stays green.

Every screen: origin 193 ARL, destination 089 DLT.

The van screens stay the van, trailer 516335, weight blank. The pup screens stay pup, dolly, pup, a blank weight to the right of each pup, total offset right, no extra pup line, no pounds. Do not turn the van into a pup set.


## Mix the loads across the six, 11:08 AM CT

Chris hearted the color theme picker. Keep that box. Do not redesign it.

He does not need a pup set and a van for each color. Across the six screens, mix them. Some show a van. Some show a pup set. Use different origin and destination pairs so the set has variety. Pull those pairs from the sample file at /workspace/agent-backups/tinkabot/estes-terminal-examples.json. Show the alpha code beside the terminal number.

Trailer rules stay. A number starting with 5 is a 53-foot van. A number starting with 2 is a 24-foot pup. A number starting with 9 is a pup. Pup order is pup, dolly, pup, blank weight to the right of each pup, total offset right. No invented pounds. No extra pup line.

Gray stays readable, no heavy overlay. Estes stays gold field, red edges, black route card, green authorized bar, chip labeled Estes. The other names stay White, Da Vinci, Contrast, Night, Gray.


## Signed concept pass, 11:12 AM CT

Chris hearted the latest color screens as concept art reference. That includes the gray screen and the light smear on its right edge. Do not fix the smear. It is not production art, and it would not happen in a real build.

Do not overwrite these files:

- estes-theme-white.png 6a586f5afd8f3184abec1d4a57c702d8
- estes-theme-parchment.png 52e22cb1c6398dd137104700f5d2fea4
- estes-theme-contrast.png fe118832d927fad87f741b39191f0a9e
- estes-theme-black.png 33dfe6736ed9ce31048a765280aad580
- estes-theme-gray.png ac3353f8e4cbb0fb8e5fa824970d22a1
- estes-theme-styled.png d3b47734008c95216616689b633c4064

The mix of van and pup across colors is extra boards, new files, not a replacement of this set.


## Notes for a later rebuild, 11:13 AM CT

Do not repaint the signed concept set.

If that gray screen is ever rebuilt, the right edge fuzzed and did not render clean. Do that edge better. Chris is not asking for that fix now.

The buttons are over-highlighted across the screens. On the normal app, buttons are the same color unless highlighting the important ones is warranted. In a future pass, tone the highlighting down so the screen looks like the real app: uniform button colors, selective emphasis only where it matters.


## Sixth screen, new file, 11:15 AM CT

The mix is five screens. The set needs six, one for each name: White, Da Vinci, Contrast, Night, Gray, Estes.

Add the Estes one as a new file. Do not overwrite estes-theme-styled.png. Any trailer type, van or pup set. Any origin and destination from the sample file. Same mix as the other five. Weights blank. No pounds.

Going forward, a new version goes in a new file beside the old one. Do not replace a file Chris has already seen.
