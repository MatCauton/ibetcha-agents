The app we're going to create is called iBetcha. Its an app that makes the user place bets with their friends, for example who drink 0.5l of water the fastest or who is going to win the next cyclocross race or who can run the fastest, etc.
The app should have the following functionality:
- user can login with username and password or via oAuth via Google or Apple (make sure that everything is secure and that passwords are stored encrypted etc)
- user can search for and add friends
- user can create a new bet (a bet has a title, a description, one or more people, a price, a date, the name of the jury who needs to approve). All people involved should get a push notification
- user can accept or decline a bet. If the user accepts, they will get a push notification and the bet will be added to their list of bets. If the user declines, they will get a push notification and the bet will be removed from their list of bets.
- the price of the bet can be money, but it can also be something else, like a beer or a pizza or having to do the dishes or having to do an task for the winner. The app should be flexible enough to allow for different types of bets.
- the jury can not be a participant in the bet, but they can be a friend of one of the participants. The jury needs to approve the bet before it can be accepted by the participants. If no jury is appointed, all participants need to approve the bet before it can be accepted.
- user can mark a bet as completed. User needs to provide data like who won the bet and possible evidence (picture or movie), and all people involved will get a push notification. The person appointed as jury has to approve the outcome.
- users can check the bets that are going on between their friends, and check which bets have been won by whom.
- users can check and update their profile
- a bet can de canceled or closed by the creater, but only if it has not been accepted by all participants yet. If a bet is canceled or closed, all participants will get a push notification.
- A bet can be changed by the creator, but only if it has not been accepted by all participants yet. And only after all participants have been notified and accepted the change.
- Push notifications: should be mobile push notifications, and should be sent to all participants of a bet when a new bet is created, when a bet is accepted or declined, when a bet is marked as completed, when a bet is canceled or closed, and when a bet is changed.
- Profile contents: username, profile picture, bio, list of friends, list of bets (active and completed), list of won bets, list of lost bets, etc.
- Evidence has a max size of 50MB and can be a picture or a video. The app should be able to compress and handle the upload and storage of evidence in a secure way to AWS S3.
- Multi language support: the app should support multiple languages, and the user should be able to switch between languages in the app. The app should be designed in a way that makes it easy to add new languages in the future. Start with English.