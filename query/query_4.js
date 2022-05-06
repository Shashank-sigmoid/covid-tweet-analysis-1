// Top 100 words occurring on tweets involving corona virus on a country basis

db.covid_tweets.aggregate([
{ $project: { user_location: "$user_location", words: { $split: ["$text", " "] } } },
{ $unwind: "$words" },
{ $match : { words: { $nin: ["a", "I", "are", "is", "to", "the", "of", "and", "in", "RT", "was", "on" , "for"]} } },
{ $group: { _id: { location: "$user_location", tweet: "$words"}, total: { "$sum": 1 } } },
{ $sort: { total : -1 } },
{ $limit: 100 }
])