// Overall number of tweets per country on a daily basis

db.covid_tweets.aggregate([
{ $match: { "created_at": { $gte: "$now" } } },
{ $group: { _id: "$user_location", tweet_count: { $sum: 1 } } }
])