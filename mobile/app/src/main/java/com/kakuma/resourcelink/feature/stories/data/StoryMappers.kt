package com.kakuma.resourcelink.feature.stories.data

import com.kakuma.resourcelink.core.database.entity.OfflineArticleEntity
import com.kakuma.resourcelink.core.database.entity.StoryEntity
import com.kakuma.resourcelink.feature.stories.domain.Author
import com.kakuma.resourcelink.feature.stories.domain.Comment
import com.kakuma.resourcelink.feature.stories.domain.Story
import com.kakuma.resourcelink.feature.stories.domain.StoryCategory
import com.kakuma.resourcelink.feature.stories.domain.StoryDetail
import com.kakuma.resourcelink.feature.stories.domain.StoryStatus

const val STORY_FEATURED_LIST = "story:featured"

fun AuthorDto.toDomain() = Author(id, fullName, avatar, country)

fun StoryDto.toEntity(listKey: String, position: Int) = StoryEntity(
    listKey = listKey, id = id, position = position, title = title, excerpt = excerpt, coverImage = coverImage,
    category = category, language = language, status = status, authorId = author.id, authorName = author.fullName,
    authorAvatar = author.avatar, authorCountry = author.country, readingTime = readingTime, likesCount = likesCount,
    commentsCount = commentsCount, isLiked = isLiked, isBookmarked = isBookmarked, publishedAt = publishedAt,
)

fun StoryEntity.toDomain() = Story(
    id = id, title = title, excerpt = excerpt, coverImage = coverImage, category = StoryCategory.from(category),
    status = StoryStatus.from(status), author = Author(authorId, authorName, authorAvatar, authorCountry),
    readingTime = readingTime, likesCount = likesCount, commentsCount = commentsCount, isLiked = isLiked,
    isBookmarked = isBookmarked, publishedAt = publishedAt,
)

fun StoryDto.toStory() = toEntity("", 0).toDomain()

fun StoryDto.toDetail() = StoryDetail(toStory(), body.orEmpty(), rejectionReason.orEmpty())

fun StoryDto.toOfflineArticle(now: Long) = OfflineArticleEntity(
    id = id, title = title, body = body.orEmpty(), excerpt = excerpt, coverImage = coverImage, category = category,
    authorId = author.id, authorName = author.fullName, authorAvatar = author.avatar, authorCountry = author.country,
    readingTime = readingTime, likesCount = likesCount, commentsCount = commentsCount, isLiked = isLiked,
    isBookmarked = isBookmarked, publishedAt = publishedAt, savedAt = now,
)

fun OfflineArticleEntity.toStory() = Story(
    id = id, title = title, excerpt = excerpt, coverImage = coverImage, category = StoryCategory.from(category),
    status = StoryStatus.PUBLISHED, author = Author(authorId, authorName, authorAvatar, authorCountry),
    readingTime = readingTime, likesCount = likesCount, commentsCount = commentsCount, isLiked = isLiked,
    isBookmarked = isBookmarked, publishedAt = publishedAt,
)

fun OfflineArticleEntity.toDetail() = StoryDetail(toStory(), body, "", isOfflineCopy = true)

fun CommentDto.toDomain() = Comment(id, author.toDomain(), body, createdAt)
