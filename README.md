# MyYelp
## Technical Stack

### Backend

- **Framework**: Spring Boot 2.x, Spring MVC
- **Database**: MySQL, MyBatis-Plus
- **Caching**: Spring Data Redis, Redission
- **Messaging**: RabbitMQ
- **Utilities**: Hutool
- **Containerization**: Docker

### Frontend

- **Technologies**: HTML, CSS, JavaScript
- **Framework**: Vue 2
- **UI Library**: Element-UI
- **HTTP Client**: Axios

## Key Features

1. **SMS Login**:
   - **Distributed Session Management**: Implemented using Redis to synchronize login states across clusters.
   - **Efficient User Data Storage**: Switched from using String to Hash for storing user information.
2. **Shop Query Optimization**:
   - **Caching Mechanism**: Utilizes Redis to cache frequently accessed shop data, reducing database load and improving query efficiency by 90%.
   - **Generic Caching Access**: Developed a static method for caching using generics and functional programming, tackling issues like cache avalanche and cache penetration.
   - **Redis Key Management**: Constant classes manage Redis key prefixes and TTL, ensuring business isolation in key space and minimizing conflicts.
3. **Geo-based Merchant Query**:
   - **Data Structures**: Utilizes Redis's Geo and Hash structures for storing nearby merchant data.
   - **Efficient Querying**: Implements high-performance merchant queries and distance-based sorting using the Geo Search command.
4. **User Interaction Features**:
   - **Likes and Ranking**: User likes stored using Redis List, with a ranking system based on ZSet for TopN likes.
   - **Social Features**: Implements user follow and mutual follow functions using Redis Set.
5. **Real-Time Feed Stream for Followers**:
   - **Implementation**: Leveraged a push-based model to ensure timely delivery of new review messages and reduce waiting time for users, suitable for systems with a smaller user base.
6. **Coupon Flash Sale**:
   - **Inventory Pre-Check**: Utilizes Redis and Lua scripting for stock pre-checks.
   - **Asynchronous Order Creation**: Implemented using RabbitMQ, solving over-selling issues and ensuring one order per user.
