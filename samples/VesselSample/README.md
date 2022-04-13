## Vessel Sample App

The Vessel Sample app showcases some of Vessel's features and follows Android's best practices guidelines.

In this repository, you will see:

- Kotlin Coroutines for background operations.
- Single-activity architecture
- Jetpack Navigation component to manage fragment transactions
- MVVM architecture using a ViewModel for each view/feature, with views observing the state through livedata
- Repositories that expose suspend functions and Kotlin flows
- Retrofit for API calls
- Koin for Dependency Injection
- Unit tests

## App Specification
The Vessel sample app is meant to be very simple and focused mainly on:
- Demonstrating Vessel's features
- Showcasing best practices

The app's main theme is a workout app that tracks a percentage score for your daily activities. You can also
view your friends, their recent activity throughout the day and posts as well. Data such as the user's stats, 
User ID and a cache of user data is all stored in Vessel.


## Setup
- Vessel requires a Github user and token to be able to authenticate and pull packages. The VesselSample project assumes that these are stored
  within environment variables under `GITHUB_USER` and `GITHUB_TOKEN`, or can be modified in the `settings.gradle` as needed
- Clone the Vessel repository `git clone git@github.com:textnow/vessel.git` and open up the samples/VesselSample project in Android Studio
- Build and install the app onto a device or emulator

## Credits

- https://jsonplaceholder.typicode.com the mock API service
- https://avatars.dicebear.com/ for mock avatars

