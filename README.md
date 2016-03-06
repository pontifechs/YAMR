# YAMR
Welcome to YAMR: Yet another Manga Reader. I was motivated to make this when I had enough itches to scratch.

Design Philosophy

1. YAMR shouldn't require anything other than the phone and the provider to read manga.
  - One of the main complaints I had with PocketManga was that it relied on additional servers aside from the actual provider. When they went down for maintenance or what have you, you couldn't read manga.
  - YAMR does not. If the provider's website is up, and your phone has network, you can read new manga.
1. YAMR should be able to add new providers easily.
  - YAMR's infrastructure is set up with a plugin-style architecture. Eventually the idea is to be able to add new providers without needing to update the app.
1. YAMR should always keep the page images.
  - When YAMR downloads a page, it keeps it. Why should you have to download a page more than once?

<img src="/screenshots/main.png" width="400"/>

## First steps
When you first load up YAMR, you'll need to get some series provided. Clicking on either of the providers (more to come eventually) will load all the series that provider has.

<img src="/screenshots/provider-viewer.png" width="400"/>

<img src="/screenshots/loading-provider.png" width="400"/>

## Series Browsing
Once it's loaded, you'll be able to browse through all of the series that provider has. A couple things to note. You can search for a given series with the search button. And you can also force it to reload all the series from a provider with the refresh button. (you'll probably never need to do this)

<img src="/screenshots/series-viewer.png" width="400"/>

<img src="/screenshots/series-search.png" width="400"/>

## Viewing a Series
When you click any of the series in the list, it'll load the chapters in that series, as well as some general info about the series.

<img src="/screenshots/one-piece.png" width="400"/>

There are a number of options available on the series page. 

1. Favorite: Add this series to your favorites.
1. Track: Track your reading in this series.
1. Refresh: Reload the chapters in this series. (again, probably won't need to do this)
1. Download All: This will pre-download all of the chapters in this series. You can then read the entire series offline.
1. Reset Progress: When a series is favorited, it will keep track of where you left off reading. This will reset
1. Delete Pages: This will delete the entire series from your phone. No page images will remain.

<img src="/screenshots/series-menu.png" width="400"/>

## Favorites
All series you have favorited will appear here. Single clicking on a series will take you right to where you left off reading.
In addition, YAMR will occasionally check with the provider to see if new chapters have been uploaded. You'll get a notification, and the series will be highlighted here.

<img src="/screenshots/favorites.png" width="400"/>

Long clicking on a chapter will bring you here, where you can see each individual page. (Pages you haven't downloaded will appear with the page not found image)
From here, clicking on an individual page, or single clicking the chapter on the previous screen will get you right into reading. 

<img src="/screenshots/page-select.png" width="400"/>

## Reading
Once you're on a page, you can get reading. Flinging left or right will take you to the previous or next page respectively. 
You can scroll and zoom the image, and long clicking will reset the zoom and scroll

<img src="/screenshots/page-viewer.png" width="400"/>

## Settings
On the main menu, there is an option which will take you to the settings page.

<img src="/screenshots/main-menu.png" width="400"/>

<img src="/screenshots/settings.png" width="400"/>

Explanations

1. Read Right-to-Left
  * This will reverse the page directions. This will make flinging the image to the right go to the next page. (As if you were reading a tankobon).
1. Pre-fetching
  * While reading, YAMR will download this many pages ahead. The value ranges from 0 to 10. 
  * 0 pages will have YAMR not pre-fetch any pages. When you attempt to view a page, it will go download it.
  * 3 page for example, will load the next 3 pages ahead of where you're reading, so that when you attempt to view the page, it is already downloaded.
1. Use External Storage
  * By default, YAMR will store downloaded pages in its own internal storage. Enabling this option tells YAMR to store it in external storage.
    * This does require you to allow YAMR to have permission to do this. On Anrdoid 6.0 and later, revoking this permission after giving it will cause YAMR to stop working.



