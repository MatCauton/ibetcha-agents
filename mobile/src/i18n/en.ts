const en = {
  // General
  appName: 'iBetcha',
  loading: 'Loading...',
  error: 'Something went wrong',
  retry: 'Retry',
  cancel: 'Cancel',
  save: 'Save',
  done: 'Done',
  send: 'Send',
  confirm: 'Confirm',
  delete: 'Delete',
  search: 'Search',
  or: 'or',

  // Auth
  login: 'Sign In',
  register: 'Create Account',
  email: 'Email',
  password: 'Password',
  username: 'Username',
  displayName: 'Display Name',
  forgotPassword: 'Forgot password?',
  noAccount: "Don't have an account?",
  hasAccount: 'Already have an account?',
  signUp: 'Sign Up',
  signIn: 'Sign In',
  loginError: 'Incorrect email or password',
  registerError: 'Could not create account',

  // Tabs
  tabHome: 'Bets',
  tabFriends: 'Friends',
  tabProfile: 'Profile',

  // Home / Bets
  myBets: 'My Bets',
  noBets: 'No bets yet',
  noBetsDescription: 'Challenge a friend to get started!',
  createBet: 'Create Bet',
  activeBets: 'Active Bets',
  pendingBets: 'Pending',
  resolvedBets: 'Resolved',

  // Bet Status
  statusPendingAcceptance: 'Pending Acceptance',
  statusActive: 'Active',
  statusPendingApproval: 'Pending Approval',
  statusPendingJury: 'Awaiting Jury',
  statusResolved: 'Resolved',
  statusDisputed: 'Disputed',
  statusExpired: 'Expired',
  statusCancelled: 'Cancelled',

  // Bet Actions
  accept: 'Accept',
  decline: 'Decline',
  markComplete: 'Mark Complete',
  selectWinner: 'Select Winner',
  approve: 'Approve',
  dispute: 'Dispute',
  concede: 'Concede',
  appointJury: 'Appoint Jury',

  // Bet Creation
  pickFriend: 'Pick a friend',
  betDescription: 'What\'s the bet?',
  betDescriptionPlaceholder: 'e.g., I\'ll beat you on Sunday\'s ride',
  betStake: 'What\'s at stake?',
  betStakePlaceholder: 'e.g., Loser buys coffee',
  sendBet: 'Send Bet',
  moreOptions: 'More options',

  // Friends
  myFriends: 'Friends',
  noFriends: 'No friends yet',
  noFriendsDescription: 'Search for friends or invite them to iBetcha!',
  addFriend: 'Add Friend',
  inviteFriend: 'Invite Friend',
  friendRequests: 'Friend Requests',
  pendingRequests: 'Pending Requests',
  searchFriends: 'Search by username...',
  noResults: 'No users found',
  friendAdded: 'Friend request sent!',

  // Profile
  myProfile: 'Profile',
  editProfile: 'Edit Profile',
  wins: 'Wins',
  losses: 'Losses',
  winRate: 'Win Rate',
  streak: 'Streak',
  totalBets: 'Total Bets',
  recentBets: 'Recent Bets',
  topRivals: 'Top Rivals',
  headToHead: 'Head to Head',
  challenge: 'Challenge',
  logout: 'Log Out',

  // Bet Detail
  betDetail: 'Bet Detail',
  participants: 'Participants',
  stake: 'Stake',
  createdBy: 'Created by',
  deadline: 'Deadline',
  evidence: 'Evidence',
  outcome: 'Outcome',
  winner: 'Winner',
  you: 'You',
  vs: 'vs',

  // Friend Profile
  challengeUser: 'Challenge {name}',
  activeBetsTitle: 'Active Bets',
  recentResults: 'Recent Results',

  // Errors
  networkError: 'Network error. Please check your connection.',
  sessionExpired: 'Session expired. Please log in again.',
  serverError: 'Server error. Please try again later.',
} as const;

export default en;

export type TranslationKey = keyof typeof en;
