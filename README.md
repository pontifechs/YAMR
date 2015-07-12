
Things I don't like.
====================

Code Smells
-----------

#### General
* I want to run some of this through some static analysis. Maybe there's one that's android specific as well that can help with lifecycle BS?]
* I kinda want to try Kotlin. Some of the battles I had been losing against android/Java I could probably solve with lambdas.

#### DBProvider.java
* The implicit synchronization problem between the URI format the model creates and the UriMatcher matching
* `query` is kind of a god method with lots of duplication inside it. Almost want to create a wrapper around the android SQLite stuff that gets rid of all the nulls when I don't care about them.

#### Series.java
* is `progressChapterId` really necessary? I think in all places I use the page, I get the chapter from querying the page. And it's a potential synchronization pitfall

#### MangaPandaFetcher.java
* I think this file will eventually go the way of the dodo. The Rhino stuff should replace it entirely.
* None of the fetches handle failures gracefully at all. The `catch //shrug? ` needs to be communicated to back to the user.
* fetchNew doesn't quite have the behavior I'd really like it to have.

#### FetcherAsync.java
* Not a huge fan of all this intent boilerplate and synchronization here and in the fragments.
* Probably will need to add `FETCH_X_FAILED` for all of these, which makes them start to be more boilerplate as straight strings. Maybe some kind of extended enum?

#### FetcherSync.java
* `NotifyStatus` is kind of all or nothing. Maybe split out into separate interfaces? The registration step gets more obnoxious though.
* Use of `stripBadCharsForFile` isn't super great. It has some pretty serious implications for file storage. If I ever change that regex in prod, I have to write a migration that moves all the images and updates the paths in the DB. Either want to do something a bit more flexible, or make really really sure it won't ever ever need to change.
* Still haven't moved `saveThumbnail` to use `downloadImage`
* I don't really like how `savePageImage` is updating the DB
* Similar to the fetcher functions, there's practically 0 error handling here. I need to go through and find all the ways this might break and deal with it.
* Maybe this heritage query should be its own object? I know we use it only one other place, which is a bit obnoxious

#### Navigation.java
* See `FetcherAsync` comments on action construction.
* Whatever error handling we use for page fetch we'll want to replicate for these `X_PAGE_DOESNT_EXIST` uses.

#### Favorites.java
* Might look into using the `heritage` stuff here in `bindView`. The join is maybe more efficient than 2 queries? Does it matter?

#### MainActivity.java
* This switch on `position` is a little obnoxious. Not sure if it's worth fighting android on though. I've not won very many of those types of battles.

#### PageViewer.java
* Again, this is similar across all the fragments. Rather than string argument keys, make a method that either creates and fills in this bullshit or creates the bundle at least.
* Really need to figure out full-screen and hiding/showing the action bar. This ties into Reader.java quite a bit as well, I think.
* Maybe collapse all these BroadcastReceivers down onto the class itself? Then dispatch similar to how the IntentServices dispatch.
* Decent amount of duplication in the next/prev failed receivers. Also the strings need to find their way into `strings.xml`
* Probably should do a dispatch type thing on the `bundle.getParcelable` arguments. If those stay in the bundle anyway. (Not sure if that has implications on the save/restore lifecycle)

#### ProviderViewer.java
* Probably need to check `savedInstanceState` for null. I think that's whats sometimes causing the loading dialog to pop back up when you hit back.
* 179 provider is hard-coded. Need to swap that out during/after the Rhino shit.

#### SeriesCard.java
* Have half a mind to pop this out into more of a generic view. Shouldn't be too hard.
* IntelliJ is right about all the TextViews. They can easily be locals.

#### TouchImageView.java
* Still need to center the image vertically. Shouldn't be too hard.
